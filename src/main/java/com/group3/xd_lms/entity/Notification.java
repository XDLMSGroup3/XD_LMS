package com.group3.xd_lms.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 消息通知实体类 - 对应数据库 notifications 表
 * 用于存储系统自动触发或管理员手动发送的各类提醒（如逾期催还、预约成功等）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    /**
     * 通知唯一标识 - 主键
     */
    private Long id;

    /**
     * 接收通知的用户ID - 外键，关联 users 表 id
     */
    @NotNull(message = "接收人ID不能为空")
    private Long userId;

    /**
     * 通知类型
     */
    @NotNull(message = "通知类型不能为空")
    private NotificationType type;

    /**
     * 通知标题
     */
    @NotBlank(message = "通知标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100")
    private String title;

    /**
     * 通知正文内容
     */
    @NotBlank(message = "通知内容不能为空")
    private String content;

    /**
     * 是否已读：false-未读，true-已读
     */
    @Default
    private Boolean isRead = false;

    /**
     * 通知创建时间
     */
    @Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * 扩展通知类型：转借请求
     */
    public enum NotificationType {
        Overdue,
        Reservation,
        System
    }

    // ==================== 业务辅助方法 ====================

    /**
     * 判断消息是否未读
     */
    public boolean isUnread() {
        return Boolean.FALSE.equals(this.isRead);
    }

    /**
     * 标记消息为已读
     */
    public void markAsRead() {
        this.isRead = true;
    }
}
