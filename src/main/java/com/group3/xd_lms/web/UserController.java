package com.group3.xd_lms.web;

import com.group3.xd_lms.entity.BorrowRecord;
import com.group3.xd_lms.entity.SystemSetting;
import com.group3.xd_lms.entity.User;
import com.group3.xd_lms.mapper.FineMapper;
import com.group3.xd_lms.mapper.SystemSettingsMapper;
import com.group3.xd_lms.mapper.BorrowRecordMapper;
import com.group3.xd_lms.mapper.UserMapper;
import com.group3.xd_lms.utils.JwtUtils;
import com.group3.xd_lms.utils.Result;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;


@RestController
@RequestMapping("/users")
public class UserController {

    private final UserMapper userMapper;
    private final BorrowRecordMapper borrowRecordMapper;
    private final SystemSettingsMapper systemSettingsMapper;
    private final FineMapper fineMapper;
    public UserController(UserMapper userMapper,BorrowRecordMapper borrowRecordMapper,SystemSettingsMapper systemSettingsMapper,FineMapper fineMapper) {
        this.userMapper = userMapper;
        this.borrowRecordMapper = borrowRecordMapper;
        this.systemSettingsMapper = systemSettingsMapper;
        this.fineMapper = fineMapper;
    }
    /**
     * 用户注册接口
     * URL: POST /users/register
     * 功能：允许新用户自主注册账号。默认设置为“读者”角色 (RoleID: 3) 且状态为“活跃”。
     *
     * @param user 包含注册信息的实体对象 (需提供 user_account, password, fullName, email)
     * @return HashMap<String, Object> 注册结果状态及用户信息
     */
    @PostMapping("/register")
    public HashMap<String, Object> register(@RequestBody User user) {
        // 1. 基础非空校验
        if (user.getUser_account() == null || user.getUser_account().trim().isEmpty()) {
            return Result.getResultMap(400, "注册失败：账号不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return Result.getResultMap(400, "注册失败：密码不能为空");
        }
        if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
            return Result.getResultMap(400, "注册失败：姓名不能为空");
        }

        // 2. 账号唯一性检查
        // 防止数据库 user_account 字段冲突
        User existingUser = userMapper.selectByUserAccount(user.getUser_account());
        if (existingUser != null) {
            return Result.getResultMap(409, "注册失败：该账号已被占用");
        }

        // 3. 设置注册用户的默认属性
        // 默认角色设为 3 (读者 Reader)
        user.setRoleId(3);
        // 默认状态设为 Active (正常使用)
        user.setStatus(User.UserStatus.Active);

        // 如果你的数据库没有设置自动生成时间，也可以在 Java 层设置
        // user.setCreatedAt(new LocalDateTime());

        // 4. 执行持久化操作
        try {
            int rows = userMapper.insert(user);
            System.out.println(rows);
            if (rows > 0) {
                // 5. 注册成功处理
                // 安全起见，返回给前端的对象中抹除密码
                user.setPassword(null);
                return Result.getResultMap(200, "注册成功", user);
            } else {
                return Result.getResultMap(500, "注册失败：服务器保存数据时出错");
            }
        } catch (Exception e) {
            // 捕获可能的数据库异常（如字段长度超出等）
            return Result.getResultMap(500, "注册异常：" + e.getMessage());
        }
    }

    /**
     * 获取当前登录用户的状态
     * URL: GET /users/status
     * 功能：前端传入userId,后端根据userId返回状态
     *
     * @param userId 用于获取当前登录用户 ID
     * @return HashMap<String, Object> 包含用户实体的通用结果
     */
    @GetMapping("/status")
    public HashMap<String, Object> GetUserStatus(@RequestParam Integer userId) {
        User res = userMapper.selectById(Long.valueOf(userId));
        return Result.getResultMap(200, "Get User Status Success", res.getStatus());
    }
    /**
     * 用户登录接口
     * URL: POST /users/login
     * 功能：验证账号密码，设置Session，返回用户信息及权限
     *
     * @param loginParams 包含密码和用户名
     * @return Result 封装的登录结果
     */
    @PostMapping("/login")
    public HashMap<String, Object> login(
            @RequestBody Map<String, String> loginParams) {

        // 2. 从 Map 中获取前端传来的 key
        String user_account = loginParams.get("user_account");
        String password = loginParams.get("password");

        // 3. 参数基础校验 (加上非空判断，防止 get 拿到 null)
        if (user_account == null || password == null ||
                user_account.trim().isEmpty() || password.trim().isEmpty()) {
            return Result.getResultMap(400, "账号或密码不能为空");
        }

        // 2. 根据账号查询用户
        User user = userMapper.selectByUserAccount(user_account);

        // 3. 用户不存在
        if (user == null) {
            return Result.getResultMap(404, "用户不存在");
        }

        // 4. 检查账号状态
        if (user.getRoleId()==2 && !user.isActive()) {
            return Result.getResultMap(403, "The Account is Disabled,Please Contact Administrator");
        }

        // 5. 密码验证
        if (!password.equals(user.getPassword())) {
            return Result.getResultMap(401, "The Account/Password is incorrect");
        }

//        // 6. 登录成功：写入 Session
//        session.setAttribute("userId", user.getId());
//        session.setAttribute("userAccount", user.getUser_account());
//        session.setAttribute("roleId", user.getRoleId());
        String token = JwtUtils.generateToken(user.getId(), user.getUser_account(), user.getRoleId());
        // 权限列表逻辑...
        List<String> permissions = new ArrayList<>();
        if (user.isAdmin()) {
            permissions.add("admin");
            permissions.add("librarian");
            permissions.add("reader");
        } else if (user.isLibrarian()) {
            permissions.add("librarian");
            permissions.add("reader");
        } else {
            permissions.add("reader");
        }

        // 7. 构建返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", user.getId());
        data.put("userAccount", user.getUser_account());
        data.put("fullName", user.getFullName());
        data.put("roleId", user.getRoleId()); // 确保这个 roleId 会返回给前端
        data.put("permissions", permissions);
        data.put("Status",user.getStatus());

        return Result.getResultMap(200, "Login Success", data);
    }

    /**
     * 获取当前登录用户的个人资料
     * URL: GET /users/me
     * 功能：从 Session 中获取当前用户 ID，并查询完整的用户详情
     *
     * @param userId 当前登录用户 ID
     * @return HashMap<String, Object> 包含用户实体的通用结果
     */
    @GetMapping("/me")
    public HashMap<String, Object> getCurrentUserProfile(@RequestParam Long userId) {
        if (userId == null) {
            return Result.getResultMap(401, "请先登录");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.getResultMap(404, "用户不存在");
        }
        return Result.getResultMap(200, "获取个人资料成功", user);
    }

    /**
     * 用户更新自己的基本资料
     * URL: PUT /users/me
     * 功能：允许当前登录用户修改自己的基础信息（如姓名、邮箱）
     *
     * @param user 前端传入的用户实体对象
     * @param session 会话对象，用于获取操作者的用户 ID
     * @return HashMap<String, Object> 返回更新操作的结果状态
     */
    @PutMapping("/me/update")
    public HashMap<String, Object> updateCurrentUserProfile(
            @RequestBody User user,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.getResultMap(401, "请先登录");
        }
        // 只能修改自己的信息
        user.setId(userId);
        int rows = userMapper.updateById(user);
        return rows > 0 ? Result.getResultMap(200, "资料更新成功") : Result.getResultMap(500, "资料更新失败");
    }

    /**
     * 用户修改自己的密码
     * URL: PUT /users/me/password
     * 功能：允许当前登录用户修改登录密码
     *
     * @param pwdMap 包含新密码的 Map，Key 为 "newPassword"
     * @param session 会话对象，用于获取操作者的用户 ID
     * @return HashMap<String, Object> 返回密码修改操作的结果状态
     **/
    @PutMapping("/me/password")
    public HashMap<String, Object> changePassword(
            @RequestBody Map<String, String> pwdMap,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return Result.getResultMap(401, "请先登录");
        }
        String newPassword = pwdMap.get("newPassword");
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return Result.getResultMap(400, "新密码不能为空");
        }
        int rows = userMapper.updatePasswordById(userId, newPassword);
        return rows > 0 ? Result.getResultMap(200, "密码修改成功") : Result.getResultMap(500, "密码修改失败");
    }


    /**
     * 分页查询用户列表（支持条件筛选 + 模糊搜索）
     * URL: GET /users/pageSearchAll
     * 权限: R1 (Admin)
     * 功能：根据页码、每页条数、角色、状态、关键字进行分页查询，返回列表与总数
     *
     * @param pageNum  页码（默认 1）
     * @param pageSize 每页条数（默认 10）
     * @param roleId   角色ID（可选）
     * @param status   用户状态（可选）
     * @param keyword  搜索关键字（账号/姓名，可选）
     * @return HashMap<String, Object> 返回查询结果、总数、用户列表
     */
    @GetMapping("pageSearchAll")
    public HashMap<String, Object> getUserPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        // 1. 分页参数校验
        if (pageNum < 1 || pageSize < 1) {
            return Result.getResultMap(400, "页码和每页条数必须大于0");
        }

        // 2. 在这里计算好偏移量，XML 里直接用
        int offset = pageSize * (pageNum - 1);

        // 3. 查询数据（把 offset 传进去）
        List<User> userList = userMapper.selectUserPage(pageSize, offset, roleId, status, keyword);

        // 4. 查询总数
        Integer total = userMapper.selectUserCount(roleId, status, keyword);


        // 4. 返回成功结果：包含列表、总条数、当前页码
        return Result.getListResultMap(200, "查询成功",total, userList);
    }


    /**
     * 管理员创建用户
     * URL: POST /users
     * 权限: R1 (Admin)
     * 功能：创建新用户时检查 user_account 是否已存在，若未指定密码则设置默认密码 123456
     *
     * @param userAccount 用户账号
     * @param fullName 用户姓名
     * @param email 用户邮箱 (可选)
     * @param roleId 用户角色ID (可选)
     * @param password 用户初始密码 (可选)
     * @return HashMap<String, Object> 返回创建结果及新用户信息
     */
    @PostMapping("CreateUser")
    public HashMap<String, Object> createUser(
            @RequestParam String userAccount,
            @RequestParam String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer roleId,
            @RequestParam(required = false) String password) {
        // 1. 参数基础校验
        if (userAccount.trim().isEmpty() || fullName.trim().isEmpty()) {
            return Result.getResultMap(400, "UserAccount Cant be empty");
        }
        // 2. 检查账号是否已存在
        User existingUser = userMapper.selectByUserAccount(userAccount);
        if (existingUser != null) {
            return Result.getResultMap(409, "UserAccount Already Exist");
        }
        // 3. 处理默认密码
        String finalPassword = (password == null || password.trim().isEmpty()) ? "123456" : password;
        // 4. 构建用户对象
        User newUser = User.builder()
                .user_account(userAccount)
                .fullName(fullName)
                .email(email)
                .password(finalPassword) // 设置默认密码或用户指定的密码
                .roleId(roleId != null ? roleId : 3) // 默认为读者 (3)
                .status(User.UserStatus.Active) // 默认为活跃状态
                .build();
        // 5. 执行插入
        int rows = userMapper.insert(newUser);
        if (rows > 0) {
            // 移除密码字段后再返回，避免泄露
            newUser.setPassword(null);
            return Result.getResultMap(200, "Create Success", newUser);
        }
        return Result.getResultMap(500, "Create Failed");
    }

    /**
     * 管理员更新用户信息
     * URL: PUT /users/{id}
     * 权限: R1 (Admin)
     * 功能：根据 ID 更新用户信息（包括角色、邮箱、状态等）
     *
     * @param id 路径变量，目标用户 ID
     * @param newUser 前端传入的包含新数据的用户实体
     * @return Map<String, Object> 返回更新操作的结果状态
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateUser(@PathVariable("id") Long id, @RequestBody User newUser) {

        User oldUser = userMapper.selectById(id);
        System.out.println(newUser);
        if (oldUser == null) {
            return Result.getResultMap(500, "Search Users Failed");
        }
        if (newUser == null) {
            return Result.getResultMap(500, "Lack of Information");
        }
        if (!Objects.equals(newUser.getId(), oldUser.getId())) {
            return Result.getResultMap(500, "The updated user ID does not match the original user ID");
        }
        // 更新除ID外的所有用户信息
        oldUser.setRoleId(newUser.getRoleId());
        oldUser.setPassword(newUser.getPassword());
        oldUser.setEmail(newUser.getEmail());
        oldUser.setStatus(newUser.getStatus());
        oldUser.setFullName(newUser.getFullName());
        oldUser.setCreatedAt(newUser.getCreatedAt());
        int rows = userMapper.updateById(oldUser);
        return Result.getResultMap(200, "Update Success");
    }

    /**
     * 管理员删除/注销用户
     * URL: DELETE /users/{id}
     * 权限: R1 (Admin)
     * 功能：将用户状态更新为 Disabled（逻辑删除），而非物理删除
     *
     * @param id 路径变量，目标用户 ID
     * @return HashMap<String, Object> 返回删除结果
     */
    @DeleteMapping("/{id}")
    public HashMap<String, Object> deleteUser(@PathVariable Long id) {
        // 1. 参数校验
        if (id == null || id <= 0) {
            return Result.getResultMap(400, "User ID is invalid");
        }

        // 2. 查询用户是否存在
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.getResultMap(404, "User Not Found");
        }

        // 3. 检查是否有未归还的借阅记录
        List<BorrowRecord> unreturnedRecords = borrowRecordMapper.selectUnreturnedByUserId(user.getId());
        if (!unreturnedRecords.isEmpty()) {
            user.setStatus(User.UserStatus.Disabled);
            int rows = userMapper.updateById(user);
            if (rows > 0) {
                return Result.getResultMap(200, "the user has borrowing records, the delete operation shall be converted to disabling.");
            }
            return Result.getResultMap(500, "Deletion failed. Please try again.");
        }

        // 4. 检查是否有未支付的罚款记录
        BigDecimal unpaidFinesSum = fineMapper.sumUnpaidFinesByUserId(user.getId());
        if (unpaidFinesSum != null && unpaidFinesSum.compareTo(BigDecimal.ZERO) > 0) {
            // 有未支付的罚款，将用户状态设为禁用而不是删除
            user.setStatus(User.UserStatus.Disabled);
            int rows = userMapper.updateById(user);
            if (rows > 0) {
                return Result.getResultMap(200, "the user has unpaid fines (amount: " + unpaidFinesSum + "), the delete operation shall be converted to disabling.");
            }
            return Result.getResultMap(500, "Operation failed. Please try again.");
        }
        List<BorrowRecord> transfer_out = borrowRecordMapper.selectP2pTransferOutRecordsByUserId(user.getId());
        if (!transfer_out.isEmpty()) {
            user.setStatus(User.UserStatus.Active);
            return Result.getResultMap(500, "the user transfer book to other users");
        }
        try {
            fineMapper.deletePaidFinesByUserId(user.getId());
            borrowRecordMapper.deleteReturnedRecordsByUserId(user.getId());
            int rows = userMapper.deleteById(user.getId());
            if (rows > 0) {
                return Result.getResultMap(200, "the user has deleted successfully.");
            }
            return Result.getResultMap(500, "Deletion failed. Please try again.");
        } catch (DataIntegrityViolationException e) {
            user.setStatus(User.UserStatus.Disabled);
            userMapper.updateById(user);
            return Result.getResultMap(200, "Deletion failed due to related data, user has been disabled instead.");
        }
    }

    /**
     * 管理员修改用户状态
     * URL: PATCH /users/{id}/status
     * 权限: R1 (Admin)
     * 功能：快速启用或禁用用户账号
     *
     * @param id 路径变量，目标用户 ID
     * @param status 目标状态字符串 (Active 或 Disabled)
     * @return HashMap<String, Object> 返回状态更新操作的结果
     */
    @PatchMapping("/{id}/status")
    public HashMap<String, Object> updateUserStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        // 1. 参数校验
        if (id == null || id <= 0) {
            return Result.getResultMap(400, "用户ID无效");
        }
        // 2. 状态值校验 (防止非法字符注入)
        User.UserStatus targetStatus;
        try {
            // 将字符串转换为枚举，如果转换失败会抛出异常
            targetStatus = User.UserStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return Result.getResultMap(400, "状态值非法，仅支持 'Active' 或 'Disabled'");
        }
        // 3. 查询用户是否存在
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.getResultMap(404, "用户不存在");
        }
        // 4. 执行状态更新
        // 策略：构建一个只包含 id 和 status 的对象进行更新
        // 这样可以避免覆盖密码、创建时间等其他字段
        User updateEntity = User.builder()
                .id(id)
                .status(targetStatus)
                .build();
        int rows = userMapper.updateById(updateEntity); // 假设使用 MyBatis-Plus 或支持动态更新的 Mapper
        if (rows > 0) {
            return Result.getResultMap(200, "用户状态已更新为: " + status);
        }
        return Result.getResultMap(500, "状态更新失败");
    }

    /**
     * 管理员更新用户角色
     * URL: PATCH /users/{id}/role
     * 权限: R1 (Admin)
     * 功能：调整用户权限角色（如 reader 升级为 librarian/admin）
     *
     * @param id 路径变量，目标用户 ID
     * @param params 包含新角色 ID 的 Map 对象
     * @return Map<String, Object> 返回角色更新操作的结果状态
     */
    @PatchMapping("/{id}/role")
    public Map<String, Object> updateUserRole(@PathVariable("id") Long id,
                                              @RequestBody Map<String, Integer> params) {
        // 1. 获取URL中的ID
        // 从 Map 中提取新角色
        Integer newRoleId = params.get("role");
        if (newRoleId == null) {
            return Result.getResultMap(500, "Missing role parameter");
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.getResultMap(500, "Search Users Failed");
        }
        System.out.println("Update Old UserId:" + id + "  to: " + newRoleId);
        user.setRoleId(newRoleId);
        int rows = userMapper.updateById(user);
        return Result.getResultMap(200, "Update Success");
    }

    /**
     * 管理员模糊搜索用户
     * URL: GET /users/searchByPattern
     * 权限: R1 (Admin)
     * 功能：通过账号(user_account)或姓名(fullName)模糊搜索用户
     *
     * @param pattern 搜索关键词 (账号或姓名)
     * @return HashMap<String, Object> 返回包含用户列表和总数的结果
     */
    @GetMapping("/searchByPattern")
    public HashMap<String, Object> searchUsersByPattern(@RequestParam String pattern) {
        // 1. 参数校验
        if (pattern == null || pattern.trim().isEmpty()) {
            return Result.getResultMap(400, "The search keyword cannot be empty");
        }
        // 2. 执行搜索
        // 策略：在 SQL 层使用 LIKE 语句，同时匹配 user_account 和 fullName
        // 假设 Mapper 中定义了 selectByKeyword 方法

        List<User> userList = userMapper.searchByKeyword(pattern);
        // 3. 结果处理
        // 即使列表为空，也返回成功状态，只是 total 为 0
        if (userList == null) {
            userList = new ArrayList<>();
        }
        // 4. 安全处理 (可选)
        // 在返回列表前，将密码字段置空，防止敏感信息泄露
        userList.forEach(user -> user.setPassword(null));
        return Result.getListResultMap(200, "Search Success", userList.size(), userList);
    }

    /**
     * 管理员根据角色ID搜索用户
     * URL: GET /users/searchbyroleid
     * 权限: R1 (Admin)
     * 功能：根据用户角色ID（1-Admin, 2-Librarian, 3-Reader）查询用户列表
     *
     * @param roleId 角色ID
     * @return HashMap<String, Object> 返回包含用户列表和总数的结果
     */
    @GetMapping("/searchbyroleid")
    public HashMap<String, Object> searchUsersByRoleId(@RequestParam Integer roleId) {
        // 1. 参数校验
        if (roleId == null) {
            return Result.getResultMap(400, "Role ID cannot be empty");
        }
        // 2. 角色ID有效性校验 (可选)
        if (roleId < 1 || roleId > 3) {
            return Result.getResultMap(400, "Invalid role ID, only 1 (Admin), 2 (Librarian), 3 (Reader) are supported");
        }
        // 3. 执行搜索
        // 假设 Mapper 中定义了 selectByRoleId 方法
        List<User> userList = userMapper.selectByRoleId(roleId);
        // 4. 结果处理
        if (userList == null) {
            userList = new ArrayList<>();
        }
        // 5. 安全处理：移除密码字段
        userList.forEach(user -> user.setPassword(null));
        return Result.getListResultMap(200, "Query Success", userList.size(), userList);
    }

    /**
     * 管理员批量导入用户数据
     * URL: POST /users/batch
     * 权限: R2 (Admin)
     * 功能：批量导入学生/教职工信息。若数据中未指定密码，则统一设置默认密码 123456；
     *      若未指定状态，则统一设置为 Active。
     *
     * @param userList 包含多个用户对象的列表 (JSON 数组)
     * @return HashMap<String, Object> 返回批量创建的结果状态
     */
    //TODO 管理员批量导入用户数据(R2)
    @PostMapping("/batch")
    public HashMap<String, Object> batchImportUsers(@RequestBody List<User> userList) {
        if (userList == null || userList.isEmpty()) {
            return Result.getResultMap(400, "User list cannot be empty");
        }
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();
        for (User user : userList) {
            if (user.getUser_account() == null || user.getUser_account().trim().isEmpty()) {
                failCount++;
                errors.add("User account is empty");
                continue;
            }
            if (user.getFullName() == null || user.getFullName().trim().isEmpty()) {
                failCount++;
                errors.add("User account " + user.getUser_account() + ": full name is empty");
                continue;
            }
            User existingUser = userMapper.selectByUserAccount(user.getUser_account());
            if (existingUser != null) {
                failCount++;
                errors.add("User account " + user.getUser_account() + " already exists");
                continue;
            }
            if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
                user.setPassword("123456");
            }
            if (user.getRoleId() == null) {
                user.setRoleId(3);
            }
            if (user.getStatus() == null) {
                user.setStatus(User.UserStatus.Active);
            }
            int rows = userMapper.insert(user);
            if (rows > 0) {
                successCount++;
            } else {
                failCount++;
                errors.add("Failed to insert user: " + user.getUser_account());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors);
        return Result.getResultMap(200, "Batch import completed", result);
    }


    /**
     * 更新特定的全局业务规则
     * URL: PUT users/settings/getallsettings
     * 权限: R2 (Admin)
     * 功能：根据配置键更新其对应的值（例如：将每日罚款金额从 0.5 修改为 1.0）
     * 功能：展示当前系统所有的业务参数配置（如每日罚金、最大续借次数等）
     *
     * @return HashMap<String, Object> 包含配置详情的通用结果
     */
    //TODO 获取全局系统配置信息
    @GetMapping("settings/getallsettings")
    public HashMap<String, Object> getAllSettings() {
        List<SystemSetting> settings = systemSettingsMapper.selectAllSettings();
        if (settings == null || settings.isEmpty()) {
            return Result.getResultMap(404, "No settings found");
        }
        return Result.getResultMap(200, "Success", settings);
    }

    /**
     * 根据 Key 获取单个配置项的值
     * URL: GET users/settings/getbykey
     *
     * @param key 配置键名称
     * @return HashMap<String, Object> 返回对应的配置值
     */
    //TODO 通过配置键获取配置信息
    @GetMapping("/settings/getbykey")
    public HashMap<String, Object> getSettingByKey(@RequestParam String key) {
        if (key == null || key.trim().isEmpty()) {
            return Result.getResultMap(400, "Setting key cannot be empty");
        }
        BigDecimal value = systemSettingsMapper.selectValueByKey(key);
        if (value == null) {
            return Result.getResultMap(404, "Setting key not found");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("key", key);
        data.put("value", value);
        return Result.getResultMap(200, "Success", data);
    }

    /**
     * 更新系统全局规则配置
     * URL: POST /settings/update
     * 权限: R2 (Admin)
     * 功能：根据前端提交的 key 和 val 修改业务参数
     * 示例参数：key="daily_fine_amount", val="1.5"
     *
     * @param key 配置项的键名称 (对应数据库 setting_key)
     * @param val 配置项的新目标值 (对应数据库 setting_value)
     * @return HashMap<String, Object> 返回更新操作的结果状态
     */
    //TODO 修改系统参数信息
    @PostMapping("settings/update")
    public HashMap<String, Object> updateSystemRule(
            @RequestParam String key,
            @RequestParam  Float val) {
        if (key == null || key.trim().isEmpty()) {
            return Result.getResultMap(400, "Setting key cannot be empty");
        }
        if (val == null) {
            return Result.getResultMap(400, "Setting value cannot be empty");
        }
        int rows = systemSettingsMapper.updateSetting(key, val);
        if (rows > 0) {
            return Result.getResultMap(200, "Setting updated successfully");
        }
        return Result.getResultMap(404, "Setting key not found");
    }

}
