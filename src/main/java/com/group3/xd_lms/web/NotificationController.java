package com.group3.xd_lms.web;

import com.group3.xd_lms.entity.BorrowRecord;
import com.group3.xd_lms.entity.Notification;
import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.BorrowRecordMapper;
import com.group3.xd_lms.mapper.NotificationMapper;
import com.group3.xd_lms.mapper.UserMapper;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;

/**
 * 消息通知控制类
 * 处理 R2 阶段逾期催还提醒、取书通知以及读者的消息中心逻辑
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationMapper notificationMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final UserMapper userMapper;

    // 构造器注入依赖
    public NotificationController(NotificationMapper notificationMapper,
                                  BorrowRecordMapper borrowRecordMapper,
                                  UserMapper userMapper) {
        this.notificationMapper = notificationMapper;
        this.borrowRecordMapper = borrowRecordMapper;
        this.userMapper = userMapper;
    }

    /**
     * 图书管理员手动发送逾期提醒
     * URL: POST /notifications/send/overdue
     * 权限: Librarian / Admin
     * 功能：针对特定的逾期借阅记录，向读者发送催还通知。
     */
    @PostMapping("/send/overdue")
    public HashMap<String, Object> sendOverdueReminder(
            @RequestParam Long borrowRecordId,
            @RequestParam(required = false) String customMessage) {
        HashMap<String, Object> result = new HashMap<>();

        try {
            // 1. 校验借阅记录ID
            if (borrowRecordId == null) {
                result.put("code", HttpStatus.BAD_REQUEST.value());
                result.put("message", "借阅记录ID不能为空");
                return result;
            }

            // 2. 查询逾期借阅记录
            BorrowRecord borrowRecord = borrowRecordMapper.selectById(borrowRecordId);
            if (borrowRecord == null) {
                result.put("code", HttpStatus.NOT_FOUND.value());
                result.put("message", "未找到该借阅记录");
                return result;
            }

            // 3. 校验是否逾期
            LocalDateTime dueDate = borrowRecord.getDueDate();
            if (dueDate == null || dueDate.isAfter(LocalDateTime.now())) {
                result.put("code", HttpStatus.BAD_REQUEST.value());
                result.put("message", "该图书未逾期，无需发送提醒");
                return result;
            }

            // 4. 查询用户信息
            Long userId = borrowRecord.getUserId();
            User user = userMapper.selectById(userId);
            if (user == null) {
                result.put("code", HttpStatus.NOT_FOUND.value());
                result.put("message", "未找到借阅用户");
                return result;
            }

            // 5. 构建通知内容（适配User实体：fullName 字段）
            String userName = user.getFullName() != null ? user.getFullName() : "用户" + userId;
            String bookName = "图书" + borrowRecordId;
            String defaultMsg = String.format("尊敬的用户%s，您借阅的《%s》已逾期，请尽快归还！",
                    userName, bookName);
            String finalMsg = (customMessage == null || customMessage.isBlank()) ? defaultMsg : customMessage;

            // 6. 构建通知对象（完全适配你的Notification实体类）
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTitle("图书逾期提醒");
            notification.setContent(finalMsg);
            notification.setType(Notification.NotificationType.Overdue);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setIsRead(false);

            // 7. 调用自定义Mapper方法保存通知
            notificationMapper.insertNotification(notification);

            // 8. 返回成功结果
            result.put("code", HttpStatus.OK.value());
            result.put("message", "逾期提醒发送成功");
            result.put("data", notification);

        } catch (Exception e) {
            log.error("发送逾期提醒失败", e);
            result.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
            result.put("message", "发送失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 批量向所有逾期用户发送提醒
     */
    @PostMapping("/send/overdue/batch")
    public HashMap<String, Object> batchSendOverdueReminders() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("code", HttpStatus.NOT_IMPLEMENTED.value());
        result.put("message", "批量发送功能待实现");
        return result;
    }

    /**
     * 获取当前用户的通知列表
     */
    @GetMapping("/my")
    public HashMap<String, Object> getMyNotifications(@RequestParam Long userId) {
        HashMap<String, Object> result = new HashMap<>();
        try {
            result.put("code", HttpStatus.OK.value());
            result.put("data", notificationMapper.selectByUserId(userId));
            result.put("message", "获取通知列表成功");
        } catch (Exception e) {
            log.error("获取通知列表失败", e);
            result.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
            result.put("message", "获取失败");
        }
        return result;
    }

    /**
     * 标记单条通知为已读
     */
    @PatchMapping("/{id}/read")
    public HashMap<String, Object> markAsRead(@PathVariable Long id) {
        HashMap<String, Object> result = new HashMap<>();
        try {
            notificationMapper.markAsRead(id);
            result.put("code", HttpStatus.OK.value());
            result.put("message", "标记已读成功");
        } catch (Exception e) {
            log.error("标记已读失败", e);
            result.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
            result.put("message", "操作失败");
        }
        return result;
    }

    /**
     * 一键标记所有通知为已读
     */
    @PatchMapping("/read-all")
    public HashMap<String, Object> markAllAsRead(HttpSession session) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("code", HttpStatus.NOT_IMPLEMENTED.value());
        result.put("message", "一键已读功能待实现");
        return result;
    }
}