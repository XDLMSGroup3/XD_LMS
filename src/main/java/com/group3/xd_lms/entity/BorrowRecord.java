package com.group3.xd_lms.entity;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 借阅记录实体类 - 对应数据库 borrow_records 表
 * 存储图书借阅全生命周期数据，关联借阅人、借阅图书信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowRecord {

    /**
     * 借阅记录唯一标识 - 主键
     */
    private Long id;

    /**
     * 借阅用户ID - 外键，关联users表id
     */
    @NotNull(message = "借阅用户ID不能为空")
    private Long userId;

    /**
     * 关联借阅用户对象 - 多对一关联
     * @ToString.Exclude 避免toString循环引用
     */
    @ToString.Exclude
    private User user;

    /**
     * 借阅图书RFID标签号 - 外键，关联book_items表rfid_tag
     */
    @NotNull(message = "借阅图书RFID不能为空")
    private String rfidTag;

    /**
     * 关联借阅的实体图书对象 - 多对一关联
     * @ToString.Exclude 避免toString循环引用
     */
    @ToString.Exclude
    private BookItem bookItem;

    /**
     * 图书借出时间
     */
    @Default
    private LocalDateTime borrowDate = LocalDateTime.now();

    /**
     * 图书应还日期
     */
    @NotNull(message = "应还日期不能为空")
    private LocalDateTime dueDate;

    /**
     * 图书实际归还时间，为空表示图书未归还
     */
    private LocalDateTime returnDate;

    /**
     * 续借次数，对应数据库表renew_count字段
     */
    @Default
    private Integer renewCount = 0;
    /**
     * 是否通过读者间转借获得 (R2 新增)
     */
    @Default
    private Boolean isP2pTransfer = false;

    /**
     * 转借来源的用户ID (R2 新增)
     */
    private Long transferFromUserId;

    // ==================== 业务辅助方法（核心高频判断逻辑） ====================
    /**
     * 判断图书是否已归还
     */
    public boolean isReturned() {
        return this.returnDate != null;
    }

    /**
     * 判断图书是否逾期（未归还且当前时间超过应还日期）
     */
    public boolean isOverdue() {
        return !isReturned() && LocalDateTime.now().isAfter(this.dueDate);
    }


    /**
     * 判断是否可续借（未归还、未逾期，可配合系统配置的最大续借次数做扩展）
     */
    public boolean canRenew() {
        return !isReturned() && !isOverdue();
    }
}
