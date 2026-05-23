package com.group3.xd_lms.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 续借申请实体类 - 对应数据库 renewal_requests 表
 * 记录读者发起的续借申请详情、理由及馆员审批反馈
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewalRequest {

    /**
     * 申请ID - 主键
     */
    private Integer id;

    /**
     * 关联的借阅记录ID - 外键
     */
    @NotNull(message = "借阅记录ID不能为空")
    private Integer borrowRecordId;

    /**
     * 申请读者ID - 外键
     */
    @NotNull(message = "用户ID不能为空")
    private Integer userId;

    /**
     * 申请提交时间
     */
    @Default
    private LocalDateTime requestDate = LocalDateTime.now();

    /**
     * 审批状态：Pending (待处理), Approved (已批准), Rejected (已驳回)
     */
    @Default
    private RequestStatus status = RequestStatus.Pending;

    /**
     * 馆员审批备注
     */
    @Size(max = 255, message = "馆员备注长度不能超过255")
    private String librarianRemark;

    /**
     * 审批处理时间
     */
    private LocalDateTime processedAt;

    /**
     * 读者填写的续借理由
     */
    @Size(max = 255, message = "理由长度不能超过255")
    private String reason;

    /**
     * 续借数量/次数 (默认为1)
     */
    @Default
    private Integer count = 0;

    /**
     * 申请状态枚举
     */
    public enum RequestStatus {
        Pending, Approved, Rejected
    }
}