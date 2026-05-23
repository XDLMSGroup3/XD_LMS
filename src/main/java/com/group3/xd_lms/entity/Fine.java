package com.group3.xd_lms.entity;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 罚款记录实体类 - 对应数据库 fines 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Fine {

    private Long id;

    @NotNull(message = "关联用户不能为空")
    private Long userId;

    @NotNull(message = "关联借阅记录不能为空")
    private Long borrowRecordId;

    /**
     * 罚款金额
     */
    @NotNull(message = "罚款金额不能为空")
    @DecimalMin(value = "0.01", message = "罚款金额必须大于0")
    private BigDecimal amount;

    /**
     * 支付状态
     */
    @Default
    private FineStatus status = FineStatus.Unpaid;

    @Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 实际支付时间
     */
    private LocalDateTime paidAt;

    public enum FineStatus {
        Unpaid, Paid
    }

    public boolean isUnpaid() {
        return FineStatus.Unpaid.equals(this.status);
    }
}