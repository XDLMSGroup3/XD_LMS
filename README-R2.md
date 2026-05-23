
---

# LMS 数据库 R2 阶段设计文档

包含图书管理系统 R2 阶段的核心数据库设计。R2 阶段重点引入了**续借审批、逾期罚款、读者间转借（P2P）以及全局系统配置**等功能。

## 1. 数据库概览
*   **数据库名称**: `lms_db`
*   **字符集**: `utf8mb4`
*   **存储引擎**: `InnoDB`

---

## 2. 数据表详细说明

### 2.1 系统全局配置表 (`system_settings`)
用于 Admin 动态配置业务规则，无需修改代码即可调整系统参数。

| 字段名 | 类型 | 描述 | 备注 |
| :--- | :--- | :--- | :--- |
| **setting_key** | VARCHAR(50) | 配置键 (PK) | 唯一标识，如 `max_loan_days` |
| setting_value | VARCHAR(255) | 配置值 | 具体数值，如 `30` |
| description | VARCHAR(255) | 配置说明 | 解释该配置的作用 |

### 2.2 续借申请表 (`renewal_requests`)
支持“前两次自动续借，第三次起需人工审批”的业务逻辑。

| 字段名 | 类型 | 描述 | 备注 |
| :--- | :--- | :--- | :--- |
| **id** | INT | 申请唯一标识 (PK) | 自增 |
| borrow_record_id | INT | 关联借阅记录 (FK) | 对应 `borrow_records.id` |
| user_id | INT | 申请人 ID (FK) | 对应 `users.id` |
| request_date | TIMESTAMP | 提交时间 | 默认当前时间 |
| status | ENUM | 审核状态 | 'Pending', 'Approved', 'Rejected' |
| librarian_remark | VARCHAR(255) | 馆员审批备注 | 驳回理由或处理说明 |
| processed_at | DATETIME | 处理时间 | 馆员操作的时刻 |
| reason | VARCHAR(255) | 申请理由 | 读者填写的续借原因 |
| count | INT | 续借次数 | 记录当前申请是该书的第几次续借 |

### 2.3 逾期罚款表 (`fines`)
记录读者逾期产生的费用明细。

| 字段名 | 类型 | 描述 | 备注 |
| :--- | :--- | :--- | :--- |
| **id** | INT | 罚款唯一标识 (PK) | 自增 |
| user_id | INT | 逾期用户 ID (FK) | 对应 `users.id` |
| borrow_record_id | INT | 关联借阅记录 (FK) | 对应 `borrow_records.id` |
| amount | DECIMAL(10,2) | 罚款金额 | 动态计算：逾期天数 * 单价 |
| status | ENUM | 支付状态 | 'Unpaid', 'Paid' |
| created_at | TIMESTAMP | 产生日期 | 逾期后系统自动生成 |
| paid_at | DATETIME | 支付日期 | 实际缴纳费用的时刻 |

### 2.4 图书预约表 (`reservations`)
支持“1小时取书倒计时”及管理员扫码核销逻辑。

| 字段名 | 类型 | 描述 | 备注 |
| :--- | :--- | :--- | :--- |
| **id** | INT | 预约唯一标识 (PK) | 自增 |
| isbn | VARCHAR(20) | 图书 ISBN (FK) | 对应 `book_metadata.isbn` |
| user_id | INT | 预约人 ID (FK) | 对应 `users.id` |
| status | ENUM | 预约状态 | 'Waiting', 'Ready', 'Completed', 'Expired', 'Cancelled' |
| request_date | TIMESTAMP | 发起时间 | 提交预约的时刻 |
| notified_at | DATETIME | 通知时间 | 图书锁定并通知读者的时刻 |
| expiration_date | DATETIME | 取书截止时间 | `notified_at` + 1小时 |

### 2.5 消息通知表 (`notifications`)
系统消息中心，支持逾期提醒和结果通知。

| 字段名 | 类型 | 描述 | 备注 |
| :--- | :--- | :--- | :--- |
| **id** | INT | 通知唯一标识 (PK) | 自增 |
| user_id | INT | 接收人 ID (FK) | 对应 `users.id` |
| type | ENUM | 通知类型 | 'Overdue', 'Reservation', 'System' |
| title | VARCHAR(100) | 通知标题 | 消息简述 |
| content | TEXT | 通知正文 | 详细提醒内容 |
| is_read | TINYINT | 已读状态 | 0-未读, 1-已读 |

---

## 3. 现有结构扩展 (Borrow Records)
为支持 **读者间转借 (P2P Transfer)**，对 `borrow_records` 表进行了如下增强：

| 新增字段 | 类型 | 描述 | 备注 |
| :--- | :--- | :--- | :--- |
| is_p2p_transfer | TINYINT(1) | P2P 标识 | 1 表示通过转借获得，0 为柜台借阅 |
| transfer_from_user_id | INT | 转借发起人 (FK) | 对应 `users.id`，记录书籍从谁手中转来 |

---

## 4. 外键约束关系汇总

| 约束名称 | 本表字段 | 关联表(字段) | 触发场景 |
| :--- | :--- | :--- | :--- |
| `fk_renewal_borrow` | renewal_requests.borrow_record_id | borrow_records(id) | 续借申请必须基于有效借阅 |
| `fk_renewal_user` | renewal_requests.user_id | users(id) | 记录申请人 |
| `fk_fine_user` | fines.user_id | users(id) | 追踪欠款人 |
| `fk_reservation_isbn`| reservations.isbn | book_metadata(isbn) | 预约针对特定图书品种 |
| `fk_notify_user` | notifications.user_id | users(id) | 消息精准推送 |
| `fk_transfer_user` | borrow_records.transfer_from_user_id | users(id) | 追踪转借来源链路 |

---

## 5. R2 预置业务规则
执行 SQL 后，系统默认预置以下规则（可在 `system_settings` 中修改）：
1.  `max_loan_days`: **30天** (默认借阅期)
2.  `fine_per_day`: **0.5元** (每日逾期罚金)
3.  `reservation_expiry_minutes`: **60分钟** (预约到馆核销限时)

---

## 6. 使用说明
1.  请确保已存在 `lms_db` 数据库。
2.  执行脚本前，请确认先执行R1-Create,和R1-Insert.
3.  本脚本开启了 `FOREIGN_KEY_CHECKS = 0` 以确保顺序执行，执行完毕后会自动恢复。