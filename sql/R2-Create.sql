/*
================================================================
R2 阶段新增功能数据库设计
1. 续借申请表 (renewal_requests)
2. 逾期罚款表 (fines)
3. 图书预约表 (reservations) - 支持1小时取书倒计时
4. 全局业务规则配置表 (system_settings)
5. 消息通知表 (notifications) - 用于发送逾期提醒
================================================================
*/
use lms_db;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 系统全局配置表 (支持 Admin 配置规则)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `system_settings` (
  `setting_key` VARCHAR(50) NOT NULL PRIMARY KEY COMMENT '配置键: max_loan_days(借阅期), fine_per_day(日罚款), reservation_hours(取书限时)',
  `setting_value` VARCHAR(255) NOT NULL COMMENT '配置值',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '配置说明'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局业务规则配置';

-- ----------------------------
-- 2. 续借申请表 (支持 Reader 申请 & Librarian 审批)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `renewal_requests` (
  `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `borrow_record_id` INT NOT NULL COMMENT '关联的原始借阅记录ID',
  `user_id` INT NOT NULL COMMENT '申请人ID',
  `request_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '申请提交时间',
  `status` ENUM('Pending', 'Approved', 'Rejected') DEFAULT 'Pending' COMMENT '审核状态',
  `librarian_remark` VARCHAR(255) DEFAULT NULL COMMENT '管理员处理意见',
  `processed_at` DATETIME DEFAULT NULL COMMENT '管理员处理时间',
   `reason` VARCHAR(255) DEFAULT NULL COMMENT '申请续借原因',
   `count` INT DEFAULT 0 COMMENT '续借次数',
  FOREIGN KEY (`borrow_record_id`) REFERENCES `borrow_records`(`id`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='续借审批流程表';

-- ----------------------------
-- 3. 逾期罚款表 (支持 逾期查询 & 财务记录)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `fines` (
  `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL COMMENT '逾期用户',
  `borrow_record_id` INT NOT NULL COMMENT '关联的借阅记录',
  `amount` DECIMAL(10, 2) NOT NULL COMMENT '罚款金额',
  `status` ENUM('Unpaid', 'Paid') DEFAULT 'Unpaid' COMMENT '支付状态',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '生成日期',
  `paid_at` DATETIME DEFAULT NULL COMMENT '支付日期',
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`),
  FOREIGN KEY (`borrow_record_id`) REFERENCES `borrow_records`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逾期罚款明细';

-- ----------------------------
-- 4. 图书预约表 (支持 预约申请 & 1小时倒计时)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `reservations` (
  `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `isbn` VARCHAR(20) NOT NULL COMMENT '预约的图书ISBN',
  `user_id` INT NOT NULL COMMENT '预约人ID',
  `status` ENUM('Waiting', 'Ready', 'Completed', 'Expired', 'Cancelled') DEFAULT 'Waiting'
    COMMENT 'Waiting:排队中; Ready:书已到馆开始倒计时; Expired:超时未取; Cancelled:手动取消',
  `request_date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发起预约时间',
  `notified_at` DATETIME DEFAULT NULL COMMENT '图书到馆通知读者的时间',
  `expiration_date` DATETIME DEFAULT NULL COMMENT '取书截止时间(用于1小时倒计时校验)',
  FOREIGN KEY (`isbn`) REFERENCES `book_metadata`(`isbn`),
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书预约及取书时限管理';

-- ----------------------------
-- 5. 系统通知表 (支持 Librarian 发送逾期/取书提醒)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `notifications` (
  `id` INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL COMMENT '接收人',
  `type` ENUM('Overdue', 'Reservation', 'System') NOT NULL COMMENT '通知类型',
  `title` VARCHAR(100) DEFAULT NULL,
  `content` TEXT NOT NULL COMMENT '通知正文',
  `is_read` TINYINT(1) DEFAULT 0 COMMENT '已读状态',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `users`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息中心';


-- ----------------------------
-- 6. 对现有表结构的 R2 扩展修改 (关键)
-- ----------------------------

-- 修改 borrow_records 支持 P2P 转借：记录转借来源
ALTER TABLE `borrow_records`
ADD COLUMN `is_p2p_transfer` TINYINT(1) DEFAULT 0 COMMENT '是否为P2P转借产生的记录',
ADD COLUMN `transfer_from_user_id` INT DEFAULT NULL COMMENT '转借发起人ID',
ADD CONSTRAINT `fk_transfer_user` FOREIGN KEY (`transfer_from_user_id`) REFERENCES `users`(`id`);


-- ----------------------------
-- 预置 R2 业务规则初始值
-- ----------------------------
INSERT IGNORE INTO `system_settings` (`setting_key`, `setting_value`, `description`) VALUES
('max_loan_days', '30', '默认借阅时长(天)'),
('fine_per_day', '0.5', '每日逾期罚款金额'),
('reservation_expiry_minutes', '60', '图书到馆后现场取书限时(分钟)');

SET FOREIGN_KEY_CHECKS = 1;