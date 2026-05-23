package com.group3.xd_lms.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 图书预约实体类 - 对应数据库 reservations 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    private Long id;

    @NotBlank(message = "预约图书ISBN不能为空")
    private String isbn;

    @NotNull(message = "预约人不能为空")
    private Long userId;

    /**
     * 预约状态
     */
    @Default
    private ReservationStatus status = ReservationStatus.Waiting;

    @Default
    private LocalDateTime requestDate = LocalDateTime.now();

    /**
     * 通知读者取书的时间（用于计算倒计时起点）
     */
    private LocalDateTime notifiedAt;

    /**
     * 预约失效时间（由系统根据配置自动计算，例如 notifiedAt + 1小时）
     */
    private LocalDateTime expirationDate;

    public enum ReservationStatus {
        /** 等待图书归还 */
        Waiting,
        /** 图书已到馆，等待读者取书 */
        Ready,
        /** 已取书并转换为借阅记录 */
        Completed,
        /** 超时未取已失效 */
        Expired,
        /** 用户手动取消 */
        Cancelled
    }

    /**
     * 判断是否处于取书倒计时中
     */
    public boolean isInCountdown() {
        return ReservationStatus.Ready.equals(this.status)
                && expirationDate != null
                && LocalDateTime.now().isBefore(expirationDate);
    }
}
