# 图书馆管理系统

## 项目介绍
本项目是基于 SpringBoot 的图书馆管理系统，包含用户管理、图书管理、借阅管理等核心功能。

## 数据库文件
- 数据库：`lms_db`
- 建表SQL 路径：`/sql/R1-Create.sql`
- 插入测试数据SQL路径：`/sql/R1-insert.sql`

## R1数据库表结构

### 2. users 用户表
**功能**：存储所有用户信息（学号/工号、密码、个人资料、状态）
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | INT | PK, AUTO_INCREMENT | 用户ID |
| user_account | VARCHAR(50) | NOT NULL, UNIQUE | 学号/工号（唯一登录账号） |
| password | VARCHAR(255) | NOT NULL | 密码 |
| user_name | VARCHAR(100) | NULL | 姓名 |
| email | VARCHAR(100) | NULL | 邮箱 |
| role_id | INT | NOT NULL | 角色ID（关联roles表） |
| status | ENUM | DEFAULT 'Active' | 账号状态：Active / Disabled |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**外键**
- `role_id` → `roles(id)`
- 删除规则：RESTRICT（禁止删除被引用的角色）

---

### 3. book_metadata 图书元数据表
**功能**：存储图书的基础信息（一本书的所有信息）
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| isbn | VARCHAR(20) | PK | ISBN编号（图书唯一标识） |
| title | VARCHAR(255) | NOT NULL | 书名 |
| author | VARCHAR(255) | NULL | 作者 |
| category | VARCHAR(100) | NULL | 分类 |
| keywords | TEXT | NULL | 关键词 |
| publisher | VARCHAR(255) | NULL | 出版社 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**
- 全文索引：`title, author, keywords`（优化搜索）

---

### 4. book_items 图书实物表
**功能**：存储每一本实体书（RFID唯一标识）
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| rfid_tag | VARCHAR(50) | PK | RFID标签（实物唯一标识） |
| isbn | VARCHAR(20) | NOT NULL | 关联图书ISBN |
| status | ENUM | DEFAULT 'Available' | 状态：Available / Loaned / Lost / Reserved |
| location | VARCHAR(100) | NULL | 馆藏位置 |
| added_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 入库时间 |

**外键**
- `isbn` → `book_metadata(isbn)`
- 删除规则：CASCADE（元数据删除 → 实物自动删除）

---

### 5. borrow_records 借阅记录表
**功能**：记录所有借书、还书、续借、逾期行为
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | INT | PK, AUTO_INCREMENT | 记录ID |
| user_id | INT | NOT NULL | 用户ID |
| rfid_tag | VARCHAR(50) | NOT NULL | 图书RFID |
| borrow_date | DATETIME | DEFAULT CURRENT_TIMESTAMP | 借书时间 |
| due_date | DATETIME | NOT NULL | 应还日期 |
| return_date | DATETIME | NULL | 实际归还日期（未还则为空） |
| renew_count | INT | DEFAULT 0 | 续借次数 |

**外键**
- `user_id` → `users(id)`（CASCADE）
- `rfid_tag` → `book_items(rfid_tag)`（CASCADE）

## 外键与约束
- 角色保护删除
- 图书元数据与实物级联删除
- 借阅记录与用户/图书实物级联删除


## github协同开发
---
## 一、首次拉取项目（仅做1次）
### 1. 克隆仓库到本地
**IDEA 操作**：
- 打开 IDEA → 点击 `Get from VCS`
- 输入仓库地址：`https://github.com/zerain050424/XD_LMS.git`
- 选择本地存储路径 → 点击 `Clone`

**命令行操作**（IDEA 底部 Terminal 执行）：
```bash
git clone https://github.com/zerain050424/XD_LMS.git
cd XD_LMS
```

### 2. 同步远程分支，拉取 `dev` 分支
**IDEA 操作**：
1.  项目打开后，点击顶部菜单 `Git → Fetch`（同步远程所有分支）
2.  左上角点击当前分支（默认 `master`）→ 展开 `Remote Branches → origin`
3.  右键 `origin/dev` → 选择 `Checkout as new local branch` → 直接点 `OK`
4.  点击顶部 `Git → Pull`，拉取 `dev` 分支最新代码

**命令行操作**：
```bash
# 同步远程分支信息
git fetch origin
# 基于远程dev创建本地dev分支并切换
git checkout -b dev origin/dev
# 拉取最新代码
git pull origin dev
```

### 3. 创建自己的功能分支
**IDEA 操作**：
- 左上角点击 `dev` 分支 → 选择 `New Branch`
- 分支名严格按规范：`feature-模块名-姓名`（例：`feature-Reader-xiaorunze`）
- 勾选 `Checkout branch` → 点击 `Create`

**命令行操作**：
```bash
# 确保在dev分支上创建
git checkout dev
git checkout -b feature-你的模块-你的姓名
# 示例：git checkout -b feature-user-zhaowentong
```

---
## 二、日常开发流程
### 1. 开发前：同步最新 `dev` 代码（避免冲突！）
**IDEA 操作**：
1.  确认左上角是自己的 `feature` 分支
2.  点击顶部 `Git → Pull` → 弹出窗口中，`Remote branch to pull` 选择 `origin/dev`
3.  点击 `Pull`，自动合并最新代码

**命令行操作**：
```bash
# 确保在自己的feature分支
git checkout feature-xxx-xxx
# 拉取dev最新代码到本地
git pull origin dev
```

### 2. 开发代码：写自己负责的接口/功能
- 只修改自己负责的文件，**绝对不要动别人的代码**
- 写完代码后，本地测试确保能正常运行、无报错

### 3. 提交代码到本地
**IDEA 操作**：
1.  点击左上角去提交修改（或右键项目 → `Git → Commit Directory`）
2.  勾选自己修改的文件（不要全选，避免误提交）
3.  提交信息严格按规范：`模块: 功能描述`（例：`user: 完成登录接口`）
4.  点击 `Commit`

**命令行操作**：
```bash
# 暂存所有修改
git add .
# 提交到本地，按规范写提交信息
git commit -m "user: 完成登录接口"
```

### 4. 推送自己的分支到远程
**IDEA 操作**：
- 点击顶部 `Git → Push` → 确认推送的是自己的 `feature` 分支 → 点击 `Push`

**命令行操作**：
```bash
# 推送到远程自己的分支
git push origin feature-xxx-xxx
```

---
## 三、提交 PR（合并到 `dev`，等待组长审核）
### 1. 发起 Pull Request
1.  推送成功后，IDEA 会弹出提示，直接点击 `View on GitHub`
2.  进入 GitHub 仓库页面，自动跳转到 PR 创建页
3.  确认：
   - `base` 分支：`dev`（目标分支，绝对不能选 `main`）
   - `compare` 分支：自己的 `feature` 分支
4.  填写 PR 信息：
   - **标题**：`[模块] 完成xx功能`（例：`[User] 完成登录接口开发`）
   - **内容**：简单说明实现的功能、测试情况（例：`完成登录接口，支持账号密码校验，已本地测试通过`）
5.  点击 `Create pull request`

### 2. 等待组长审核
- 组长会审核代码，可能会提修改意见，按要求修改后，重新推送自己的分支即可
- 代码审核通过后，组长会合并到 `dev` 分支，你的任务完成




				
				
				
				
				
