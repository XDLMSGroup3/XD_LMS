package com.group3.xd_lms.web;

import com.group3.xd_lms.dto.UnpaidFineDetailDTO;
import com.group3.xd_lms.entity.Fine;
import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.FineMapper;
import com.group3.xd_lms.mapper.BorrowRecordMapper;
import com.group3.xd_lms.mapper.SystemSettingsMapper;
import com.group3.xd_lms.entity.BorrowRecord;
import com.group3.xd_lms.mapper.UserMapper;
import com.group3.xd_lms.utils.Result;
import com.sun.jdi.LongValue;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 财务与罚款控制类
 * 处理 R2 阶段读者的逾期欠款查询、实时金额计算及缴纳确认逻辑
 */
@RestController
@RequestMapping("/fines")
public class FineController {

    private final FineMapper fineMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final SystemSettingsMapper systemSettingsMapper;
    private final UserMapper userMapper;
    
    FineController(FineMapper fineMapper, 
                   BorrowRecordMapper borrowRecordMapper,
                   SystemSettingsMapper systemSettingsMapper,
                   UserMapper userMapper) {
        this.fineMapper = fineMapper;
        this.borrowRecordMapper = borrowRecordMapper;
        this.systemSettingsMapper = systemSettingsMapper;
        this.userMapper = userMapper;
    }

    /**
     * 实时查询单笔借阅记录的预计罚款
     * URL: GET /fines/calculate/{borrowRecordId}
     * 权限: Reader
     * 业务逻辑说明：
     * 1. 后端根据 borrowRecordId 获取该笔借阅的应还时间(due_date)。
     * 2. 从 system_settings 表读取配置项 "fine_per_day" (每日罚金)。
     * 3. 计算当前日期与 due_date 的天数差。
     * 4. 计算逻辑：预计金额 = 逾期天数 * 每日罚金。
     *
     * @param borrowRecordId 前端传入的逾期借阅记录ID
     * @return HashMap<String, Object> 包含逾期天数、罚金单价及实时计算的总金额
     */
    @GetMapping("/calculate/{borrowRecordId}")
    public HashMap<String, Object> calculateOverdueFine(@PathVariable Long borrowRecordId) {
        // 1. 查询借阅记录
        BorrowRecord record = borrowRecordMapper.selectById(borrowRecordId);
        Fine fine = new Fine();
        if (record == null) {
            return Result.getResultMap(404, "Borrowing records do not exist");
        }
        
        // 2. 检查是否已归还
        if (record.isReturned()) {
            return Result.getResultMap(400, "The borrowed item has been returned, and there are no overdue penalties.");
        }
        
        // 3. 计算逾期天数
        LocalDateTime dueDate = record.getDueDate();
        LocalDateTime now = LocalDateTime.now();
        
        // 如果还未到应还日期，返回未逾期
        if (now.isBefore(dueDate) || now.isEqual(dueDate)) {
            return Result.getResultMap(400, "The borrowing record is not overdue.");
        }

        long overdueDays = ChronoUnit.DAYS.between(dueDate, now);
        if (overdueDays <= 0) {
            return Result.getResultMap(400, "The borrowing record is not overdue.");
        }
        
        // 4. 获取每日罚金配置
        BigDecimal finePerDay = systemSettingsMapper.selectValueByKey("fine_per_day");
        if (finePerDay == null) {
            return Result.getResultMap(500, "System configuration error: Daily penalty configuration not found");
        }
        
        // 5. 计算罚款金额
        BigDecimal calculatedAmount = finePerDay.multiply(BigDecimal.valueOf(overdueDays));

        // 6. 返回结果
        HashMap<String, Object> result = new HashMap<>();
        result.put("overdueDays", overdueDays);
        result.put("finePerDay", finePerDay);
        result.put("calculatedAmount", calculatedAmount);
        result.put("borrowRecordId", borrowRecordId);
        result.put("dueDate", dueDate);
        result.put("status", 200);
        result.put("message", "计算成功");
        
        return result;
    }

    /**
     * 获取当前读者所有未缴纳的欠款清单
     * URL: GET /fines/my-unpaid
     * 权限: Librarian
     * 功能：检索该读者所有已产生且未支付的罚款记录(fines表)，用于财务页面展示。
     * @param userId 用于获取当前登录用户ID
     * @return HashMap<String, Object> 返回未付罚款记录列表
     */
    //Todo 图书管理员获取该读者名下所有的未缴纳罚款
    @GetMapping("/my-unpaid")
    public HashMap<String, Object> getMyUnpaidFines(@RequestParam Integer userId) {
        HashMap<String, Object> result = new HashMap<>();
        Long userIdLong = userId.longValue();

        List<UnpaidFineDetailDTO> details = fineMapper.selectUnpaidFinesWithDetails(userIdLong);
        if (details == null) details = Collections.emptyList();

        result.put("unpaidFines", details);
        result.put("count", details.size());
        result.put("userId", userId);

        return result;
    }

    /**
     * 缴纳罚款确认
     * URL: POST /fines/pay
     * 权限: Reader
     * 业务逻辑说明：
     * 1. 读者点击“缴纳”并完成模拟支付。
     * 2. 后端修改 fines 表对应记录的 status 为 'Paid'。
     *
     * @param fineId 罚款记录ID
     * @param paymentMethod 支付方式 (如 "Balance", "AliPay" 等)
     * @return HashMap<String, Object> 返回支付操作结果
     */
    @PostMapping("/pay")
    public HashMap<String, Object> payFine(
            @RequestParam Integer fineId,
            @RequestParam String paymentMethod) {
        
        // 验证参数
        if (fineId == null || fineId <= 0) {
            return Result.getResultMap(400, "Invalid fine record ID");
        }
        
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            return Result.getResultMap(400, "Payment method cannot be empty");
        }
        
        // 更新罚款状态为Paid
        int updated = fineMapper.updatePaymentStatus(fineId.longValue(), "Paid");
        // 检查更新是否成功
        if (updated > 0) {
            return Result.getResultMap(200, "Fine paid successfully");
        } else {
            return Result.getResultMap(404, "Fine record does not exist or update failed");
        }
    }
    /**
     * 根据借阅记录查询欠款记录
     * URL: POST /fines/getbyrecord
     * 权限: Reader
     * 业务逻辑说明：
     * 根据借阅记录在数据库中查询欠款id
     *
     * @param borrowRecordId 借阅记录ID
     * @return HashMap<String, Object> 返回操作结果
     */
    @GetMapping("/calculate/getbyrecord")
    public HashMap<String, Object> GetFineByRecordId(@RequestParam Long borrowRecordId) {
        Fine fine = fineMapper.selectByBorrowRecordId(borrowRecordId);
        if(fine == null) {
            return Result.getResultMap(404, "Not Exist");
        }
        HashMap<String, Object> result = new HashMap<>();
        result.put("fine", fine);
        result.put("status", 200);
        result.put("message", "查询成功");
        return result;
    }

    /**
     * 管理员查询全馆未缴清罚款统计
     * URL: GET /fines/admin/summary
     * 权限: Librarian
     * 功能：R2 Librarian 角色查看当前图书馆的所有未收账款明细。
     */
    //Todo 图书管理员查看所有未缴纳罚款
    @GetMapping("/admin/summary")
    public HashMap<String, Object> getAdminFineSummary() {
        HashMap<String, Object> result = new HashMap<>();
        List<UnpaidFineDetailDTO> allUnpaidFines = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        List<User> users = userMapper.selectAll();
        if (users == null) users = Collections.emptyList();

        for (User user : users) {
            List<UnpaidFineDetailDTO> details = fineMapper.selectUnpaidFinesWithDetails(user.getId());
            if (details != null && !details.isEmpty()) {
                allUnpaidFines.addAll(details);
                // 累加金额（假设 DTO 中有 fineAmount 字段）
                totalAmount = totalAmount.add(
                        details.stream().map(UnpaidFineDetailDTO::getFineAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                );
            }
        }

        result.put("unpaidFines", allUnpaidFines);
        result.put("totalCount", allUnpaidFines.size());
        result.put("totalUnpaidAmount", totalAmount);
        result.put("timestamp", LocalDateTime.now());
        return result;
    }
}
