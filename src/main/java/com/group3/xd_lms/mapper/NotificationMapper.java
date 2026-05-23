package com.group3.xd_lms.mapper;

import com.group3.xd_lms.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 系统通知映射接口 - 对应数据库 notifications 表
 * 处理 R2 阶段逾期提醒、取书通知等消息的发送与展示
 */
@Mapper
public interface NotificationMapper {

    /**
     * 发送系统通知 - 对应管理员 R2 发送逾期提醒功能
     */
    int insertNotification(Notification notification);

    /**
     * 查询用户的站内信列表
     */
    List<Notification> selectByUserId(@Param("userId") Long userId);

    /**
     * 标记消息为已读
     */
    int markAsRead(@Param("id") Long id);
}