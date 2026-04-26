package com.group3.xd_lms.web;

import com.group3.xd_lms.entity.BorrowRecord;
import com.group3.xd_lms.entity.RenewalRequest;
import com.group3.xd_lms.entity.Reservation;
import com.group3.xd_lms.mapper.BorrowRecordMapper;
import com.group3.xd_lms.mapper.RenewalRequestMapper;
import com.group3.xd_lms.mapper.ReservationMapper;
import com.group3.xd_lms.utils.Result;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.CustomSQLErrorCodesTranslation;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import com.group3.xd_lms.entity.RenewalRequest.RequestStatus;

/**
 * 图书流转控制类
 * 处理 R2 阶段核心业务：续借审批、读者间直借(P2P)、图书预约及取书倒计时管理
 */
@RestController
@RequestMapping("/circulation")
public class BookCirculationController {

    private final BorrowRecordMapper borrowRecordMapper;
    private final RenewalRequestMapper renewalRequestMapper;
    private final ReservationMapper reservationMapper;

    // 构造函数注入
    public BookCirculationController(BorrowRecordMapper borrowRecordMapper,
                                     RenewalRequestMapper renewalRequestMapper,
                                     ReservationMapper reservationMapper) {
        this.borrowRecordMapper = borrowRecordMapper;
        this.renewalRequestMapper = renewalRequestMapper;
        this.reservationMapper = reservationMapper;
    }

    // ==================== 一、续借管理 (Renewal Section) ====================

    /**
     * 读者发起续借请求 (核心业务分流接口)
     * URL: POST /circulation/renew/apply
     * 权限: Reader
     * 业务逻辑说明：
     * 1. 后端查询 borrow_records 表中的 renew_count。
     * 2. 若 renew_count < 2：直接更新 borrow_records 的应还日期，返回“自动续借成功”。
     * 3. 若 renew_count >= 2：在 renewal_requests 表创建一条状态为 'Pending' 的记录，返回“已进入人工审批流”。
     *
     * @param borrowRecordId 借阅记录ID
     * @param reason 续借理由 (若次数>=2则必填，自动续借时可选)
     * @param userId 用于获取当前用户ID并校验操作权限
     * @return HashMap<String, Object> 返回处理结果（告知用户是自动续借成功，还是已提交申请）
     */
    //Todo 读者发起续借请求
    @PostMapping("/renew/apply")
    public HashMap<String, Object> applyForRenewal(
            @RequestParam Long borrowRecordId,
            @RequestParam(required = false) String reason,
            @RequestParam Integer userId) {
        try {
            // 1. 查询借阅记录
            BorrowRecord borrowRecord = borrowRecordMapper.selectById(borrowRecordId);
            if (borrowRecord == null) {
                return Result.getResultMap(404, "借阅记录不存在");
            }

            // 持有校验：只能给自己的图书续借
            if (!borrowRecord.getUserId().toString().equals(userId.toString())) {
                return Result.getResultMap(403, "无权限：只能续借自己借阅的图书");
            }

            // 已归还图书不能续借
            if (borrowRecord.getReturnDate() != null) {
                return Result.getResultMap(400, "操作失败：图书已归还");
            }

            // 2. 核心业务逻辑：判断续借次数
            int currentRenewCount = borrowRecord.getRenewCount();
            if (currentRenewCount <= 2) {
                // ==================== 场景1：次数≤2 → 自动续借 ====================
                borrowRecord.setRenewCount(currentRenewCount + 1);
                borrowRecord.setDueDate(borrowRecord.getDueDate().plusDays(30));
                borrowRecordMapper.updateDueDateAndRenewCount(borrowRecord);
                return Result.getResultMap(200, "自动续借成功！已延长30天");
            } else {
                // ==================== 场景2：次数>2 → 提交审批申请 ====================
                if (reason == null || reason.trim().isEmpty()) {
                    return Result.getResultMap(400, "续借次数超过2次，必须填写理由");
                }
                // 创建续借申请
                RenewalRequest request = new RenewalRequest();
                request.setBorrowRecordId(borrowRecordId.intValue());
                request.setUserId(userId);
                request.setReason(reason);
                request.setStatus(RenewalRequest.RequestStatus.Pending);
                request.setRequestDate(java.time.LocalDateTime.now());

                int rows = renewalRequestMapper.insert(request);
                if (rows <= 0) {
                    return Result.getResultMap(500, "提交续借申请失败");
                }
                return Result.getResultMap(200, "续借申请已提交，等待馆员审批");
            }
        } catch (Exception e) {
            return Result.getResultMap(500, "续借失败：" + e.getMessage());
        }
    }

    /**
     * 馆员获取待审批列表 (次数 > 2 次的申请)
     * URL: GET /circulation/renew/pending
     * 权限: Librarian
     * 功能：列出所有因超过续借次数限制而进入人工审批流程的请求。
     */
    //Todo 图书管理员获取待审批列表
    @GetMapping("/renew/pending")
    public HashMap<String, Object> getPendingRenewalRequests() {
        try {
            List<RenewalRequest> pendingList = renewalRequestMapper.selectByStatus("Pending");
            return Result.getListResultMap(200, "获取成功", pendingList.size(), pendingList);
        } catch (Exception e) {
            return Result.getResultMap(500, "获取失败：" + e.getMessage());
        }
    }

    /**
     * 馆员执行续借审批
     * URL: PUT /circulation/renew/audit
     * 权限: Librarian
     * 功能：针对 >= 2 次的续借申请进行人工干预。
     *      若批准，则手动更新 borrow_records 表的应还日期。
     *
     * @param requestId 续借申请单ID
     * @param isApprove 是否批准 (true/false)
     * @param remark 馆员审批意见/驳回理由
     * @return HashMap<String, Object> 返回审批操作结果
     */
    //Todo 图书管理员进行续借审批
    @PutMapping("/renew/audit")
    public HashMap<String, Object> auditRenewalRequest(
            @RequestParam Integer requestId,
            @RequestParam Boolean isApprove,
            @RequestParam(required = false) String remark) {
        try {
            // 1. 查询续借申请
            RenewalRequest request = renewalRequestMapper.selectById(requestId);
            if (request == null) {
                return Result.getResultMap(404, "续借申请不存在");
            }
            if (request.getStatus() != RequestStatus.Pending) {
                return Result.getResultMap(400, "该申请已处理，无法重复审批");
            }

            // 2. 核心校验：必须是该书当前的持有者
            BorrowRecord borrowRecord = borrowRecordMapper.selectById(request.getBorrowRecordId().longValue());
            if (borrowRecord == null) {
                return Result.getResultMap(404, "关联的借阅记录不存在");
            }
            if (!borrowRecord.getUserId().toString().equals(request.getUserId().toString())) {
                return Result.getResultMap(403, "校验失败：申请人不是该书当前持有者");
            }


            // 3. 更新申请状态
            request.setStatus(isApprove ? RequestStatus.Approved : RequestStatus.Rejected);
            request.setLibrarianRemark(remark);
            request.setProcessedAt(java.time.LocalDateTime.now());
            int updateRows = renewalRequestMapper.updateAuditStatus(request);
            if (updateRows <= 0) {
                return Result.getResultMap(500, "更新审批状态失败");
            }

            // 4. 审批通过则更新借阅记录（使用团队已有的updateDueDateAndRenewCount方法）
            if (isApprove) {
                borrowRecord.setRenewCount(borrowRecord.getRenewCount() + 1);
                borrowRecord.setDueDate(borrowRecord.getDueDate().plusDays(30));
                borrowRecordMapper.updateDueDateAndRenewCount(borrowRecord);
            }

            return Result.getResultMap(200, isApprove ? "审批通过" : "审批驳回");
        } catch (Exception e) {
            return Result.getResultMap(500, "审批失败：" + e.getMessage());
        }
    }

    

    // ==================== 二、转借管理 (Transfer Section) ====================
    /**
     * 生成转借凭证信息 (供持有者生成二维码)
     * URL: GET /circulation/transfer/qr-data
     * 权限: Reader (当前持有者)
     * 功能：校验当前用户权限并返回该笔借阅的唯一标识，用于前端生成二维码。
     *
     * @param borrowRecordId 当前有效的借阅记录ID
     * @param userId 用于校验操作者是否为该书当前真实持有人
     * @return HashMap<String, Object> 包含用于生成二维码的数据字符串
     */
    //Todo 持有者生成转借二维码
    @GetMapping("/transfer/qr-data")
    public HashMap<String, Object> getTransferQrData(
            @RequestParam Long borrowRecordId,
            @RequestParam Integer userId) {
        // 此处仅定义接口，不实现逻辑
        return null;
    }

    /**
     * 扫码确认转借 (接收者触发)
     * URL: POST /circulation/transfer/scan-confirm
     * 权限: Reader (扫码者/接收方)
     * 业务逻辑说明：
     * 1. 接收者登录系统后扫描二维码，解析出 borrowRecordId。
     * 2. 后端执行原子操作：
     *    a. 记录原持有人 ID (从旧记录获取)。
     *    b. 修改该 borrow_record 的 user_id 为扫码者 ID (从 session 获取)。
     *    c. 修改 is_p2p_transfer = 1 (标记为转借)。
     *    d. 修改 transfer_from_user_id = 原持有人 ID。
     *    e. 重置借阅时间/应还时间（可选，视图书馆规则而定）。
     *
     * @param borrowRecordId 从二维码中扫描出来的借阅记录ID
     * @param userId 用于获取扫码者（接收方）的 ID
     * @return HashMap<String, Object> 返回转借成功的最终状态
     */
    //Todo 扫码确认转借
    @PostMapping("/transfer/scan-confirm")
    public HashMap<String, Object> confirmTransferByScan(
            @RequestParam Long borrowRecordId,
            @RequestParam Integer userId) {
        // 核心逻辑：单条记录更新，修改 user_id, is_p2p_transfer 和 transfer_from_user_id
        return null;
    }


    // ==================== 三、预约管理 (Reservation Section) ====================

    /**
     * 读者预约可用图书
     * URL: POST /circulation/reserve/apply
     * 权限: Reader
     * 业务逻辑说明：
     * 1. 校验该书籍(rfidTag)当前状态是否为 'Available'。
     * 2. 在 reservations 表创建记录：status='Ready', notified_at=NOW(), expiration_date=NOW()+1h。
     * 3. 同步修改 book_items 表该书籍状态为 'Reserved'，防止他人在此期间借走。
     *
     * @param rfidTag 目标图书的RFID标签
     * @param userId 用于获取预约人ID
     * @return HashMap<String, Object> 返回预约结果及核销码所需的基础数据
     */
    //Todo 发起预约
    @PostMapping("/reserve/apply")
    public HashMap<String, Object> applyForReservation(
            @RequestParam String rfidTag,
            @RequestParam Integer userId) {
        return null;
    }

    /**
     * 获取预约核销二维码数据
     * URL: GET /circulation/reserve/qr-data
     * 权限: Reader
     * 功能：读者在 App 中点击“显示核销码”时调用，返回预约 ID 等加密信息。
     *
     * @param reservationId 预约记录ID
     * @return HashMap<String, Object> 返回用于生成核销二维码的字符串数据
     */
    //Todo 生成核销二维码
    @GetMapping("/reserve/qr-data")
    public HashMap<String, Object> getReservationQrData(
            @RequestParam Integer reservationId,
            HttpSession session) {
        // 此处仅定义接口，不实现逻辑
        return null;
    }

    /**
     * 管理员扫码核销并自动借出
     * URL: POST /circulation/reserve/verify-scan
     * 权限: Librarian
     * 业务逻辑说明：
     * 1. 管理员使用终端扫描读者出示的码，获取 reservationId。
     * 2. 后端校验：
     *    a. 预约是否在 1 小时有效期内 (NOW < expiration_date)。
     *    b. 预约状态是否为 'Ready'。
     * 3. 校验通过后执行原子操作：
     *    a. 修改预约状态为 'Completed'。
     *    b. 调用借书逻辑：向 borrow_records 插入新记录，修改 book_items 状态为 'Loaned'。
     *
     * @param reservationId 从核销码中解析出的预约ID
     * @return HashMap<String, Object> 返回最终借阅完成的状态
     */
    //Todo 管理员扫码核销
    @PostMapping("/reserve/verify-scan")
    public HashMap<String, Object> verifyAndCompleteBorrow(@RequestParam Integer reservationId) {
        // 此处仅定义接口，不实现逻辑
        return null;
    }

    /**
     * 管理员获取当前待拨/已锁定待取的图书列表
     * URL: GET /circulation/reserve/librarian/todo
     * 权限: Librarian
     * 功能：方便管理员查看有哪些书被预约了，提前准备（调拨）图书。
     */
    //Todo 管理员查看当前已被预约的图书列表
    @GetMapping("/reserve/librarian/todo")
    public HashMap<String, Object> getPendingAllocations() {
        // 此处仅定义接口，不实现逻辑
        return null;
    }
}
