SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0; 

-- ----------------------------
-- 1. 创建并使用数据库
-- ----------------------------
CREATE DATABASE IF NOT EXISTS `lms_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `lms_db`;

-- 角色表
CREATE TABLE `roles` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名: Admin, Librarian, Reader',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始化角色数据
INSERT INTO `roles` (`id`, `role_name`) VALUES (1, 'Admin'), (2, 'Librarian'), (3, 'Reader');

-- 用户主表
CREATE TABLE `users` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_account` VARCHAR(50) NOT NULL UNIQUE COMMENT '学号/工号',
  `password` VARCHAR(255) NOT NULL,
  `user_name` VARCHAR(100) DEFAULT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `role_id` INT NOT NULL,
  `status` ENUM('Active', 'Disabled') DEFAULT 'Active' COMMENT '账号状态 (禁用/删除)',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  -- 用户角色通常不随角色表删除，设为 RESTRICT 保护核心逻辑
  CONSTRAINT `fk_user_role` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 表 A: 图书元数据 (Book Metadata) - 存储书的通用信息
CREATE TABLE `book_metadata` (
  `isbn` VARCHAR(20) NOT NULL COMMENT 'ISBN',
  `title` VARCHAR(255) NOT NULL COMMENT '书名',
  `author` VARCHAR(255) DEFAULT NULL COMMENT '作者',
  `category` VARCHAR(100) DEFAULT NULL COMMENT '分类',
  `keywords` TEXT DEFAULT NULL COMMENT '关键词',
  `publisher` VARCHAR(255) DEFAULT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`isbn`),
  FULLTEXT KEY `idx_search` (`title`, `author`, `keywords`) -- 全文索引优化搜索
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书基本信息表';

-- 表 B: 实物图书复本 (Book Items) - 存储每一本具体的实体书
CREATE TABLE `book_items` (
  `rfid_tag` VARCHAR(50) NOT NULL COMMENT 'RFID标签',
  `isbn` VARCHAR(20) NOT NULL COMMENT '关联的ISBN',
  `status` ENUM('Available', 'Loaned', 'Lost', 'Reserved') DEFAULT 'Available' COMMENT '状态 (Reader 1.4)',
  `location` VARCHAR(100) DEFAULT NULL COMMENT '馆内具体位置',
  `added_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`rfid_tag`),
  -- 如果彻底下架某种图书(ISBN)，则对应的所有实体书(RFID)自动删除
  CONSTRAINT `fk_item_isbn` FOREIGN KEY (`isbn`) REFERENCES `book_metadata` (`isbn`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书实物信息表';

CREATE TABLE `borrow_records` (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `rfid_tag` VARCHAR(50) NOT NULL,
  `borrow_date` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `due_date` DATETIME NOT NULL COMMENT '应还日期',
  `return_date` DATETIME DEFAULT NULL COMMENT '实际归还日期',
  `renew_count` INT DEFAULT 0 COMMENT '续借次数',
  PRIMARY KEY (`id`),
  -- 用户删除或实体书删除，借阅记录随之清除
  CONSTRAINT `fk_borrow_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_borrow_rfid` FOREIGN KEY (`rfid_tag`) REFERENCES `book_items` (`rfid_tag`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;