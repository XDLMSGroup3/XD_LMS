package com.group3.xd_lms.web;

import com.group3.xd_lms.entity.BorrowRecord;
import com.group3.xd_lms.entity.RenewalRequest;
import com.group3.xd_lms.mapper.BorrowRecordMapper;
import com.group3.xd_lms.mapper.RenewalRequestMapper;
import com.group3.xd_lms.utils.Result;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * 续借功能控制器
 * 负责人：夏盛豪
 * API: POST /renewal/apply
 */
@RestController
@RequestMapping("/renewal")
public class RenewalController {

    private final BorrowRecordMapper borrowRecordMapper;
    private final RenewalRequestMapper renewalRequestMapper;

    public RenewalController(BorrowRecordMapper borrowRecordMapper,
                             RenewalRequestMapper renewalRequestMapper) {
        this.borrowRecordMapper = borrowRecordMapper;
        this.renewalRequestMapper = renewalRequestMapper;
    }

    /**
     * 读者发起续借申请
     * URL: POST /renewal/apply
     *
     * @param borrowRecordId 借阅记录ID
     * @param reason         续借理由（续借次数≥2时必填）
     * @param userId         用户ID
     * @return 处理结果
     */
    @PostMapping("/apply")
    @Transactional
    public HashMap<String, Object> applyForRenewal(
            @RequestParam Long borrowRecordId,
            @RequestParam(required = false) String reason,
            @RequestParam Integer userId) {

        // 1. 查询借阅记录
        BorrowRecord borrowRecord = borrowRecordMapper.selectById(borrowRecordId);
        if (borrowRecord == null) {
            return Result.getResultMap(404, "借阅记录不存在");
        }

        // 2. 权限校验：只能续借自己借阅的图书
        if (!borrowRecord.getUserId().equals(userId.longValue())) {
            return Result.getResultMap(403, "无权限：只能续借自己借阅的图书");
        }

        // 3. 已归还图书不能续借
        if (borrowRecord.getReturnDate() != null) {
            return Result.getResultMap(400, "操作失败：图书已归还");
        }

        // 4. 逾期图书不能续借
        if (borrowRecord.isOverdue()) {
            return Result.getResultMap(400, "操作失败：图书已逾期，请先归还并缴纳罚款");
        }

        int currentRenewCount = borrowRecord.getRenewCount();

        // 5. 续借次数 < 2 → 自动续借（第1次和第2次续借自动通过）
        if (currentRenewCount < 2) {
            int newCount = currentRenewCount + 1;
            LocalDateTime newDueDate = borrowRecord.getDueDate().plusDays(30);
            borrowRecord.setRenewCount(newCount);
            borrowRecord.setDueDate(newDueDate);
            borrowRecordMapper.updateDueDateAndRenewCount(borrowRecord);
            return Result.getResultMap(200, "自动续借成功！已延长30天，当前续借次数：" + newCount);
        }

        // 6. 续借次数 ≥ 2 → 人工审批（必须填写理由）
        if (reason == null || reason.trim().isEmpty()) {
            return Result.getResultMap(400, "续借次数已达" + currentRenewCount + "次，必须填写续借理由");
        }

        // 7. 禁止重复提交：检查是否已有 Pending 状态的申请
        RenewalRequest existingRequest = renewalRequestMapper.selectPendingByUserAndBorrowId(
                userId, borrowRecordId.intValue());
        if (existingRequest != null) {
            return Result.getResultMap(409, "已有待审批的续借申请，请勿重复提交");
        }

        // 8. 创建续借申请
        RenewalRequest request = RenewalRequest.builder()
                .borrowRecordId(borrowRecordId.intValue())
                .userId(userId)
                .reason(reason)
                .status(RenewalRequest.RequestStatus.Pending)
                .requestDate(LocalDateTime.now())
                .count(currentRenewCount + 1)
                .build();

        int rows = renewalRequestMapper.insert(request);
        if (rows > 0) {
            return Result.getResultMap(200, "续借申请已提交，等待馆员审批（当前续借次数：" + currentRenewCount + "）");
        }
        return Result.getResultMap(500, "提交续借申请失败");
    }
}