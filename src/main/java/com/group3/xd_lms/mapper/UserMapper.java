package com.group3.xd_lms.mapper;

import com.group3.xd_lms.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    // ==================== 基础CRUD ====================
    /**
     * 新增用户
     */
    int insert(User user);

    /**
     * 根据ID更新用户信息
     */
    int updateById(User user);

    /**
     * 根据ID查询用户
     */
    User selectById(@Param("id") Long id);

    /**
     * 根据ID删除用户
     */
    int deleteById(@Param("id") Long id);

    // ==================== 登录核心 ====================
    /**
     * 根据学号/工号查询用户（登录用）
     */
    User selectByUserAccount(@Param("userAccount") String userAccount);

    // ==================== 状态操作 ====================
    /**
     * 修改用户状态（启用/禁用）
     */
    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    /**
     * 修改密码
     */
    int updatePasswordById(@Param("id") Long id, @Param("password") String password);

    // ==================== 条件查询 ====================
    /**
     * 查询所有用户
     */
    List<User> selectAll();

    /**
     * 根据角色ID查询用户
     */
    List<User> selectByRoleId(@Param("roleId") Integer roleId);

    /**
     * 根据状态查询用户
     */
    List<User> selectByStatus(@Param("status") String status);

    /**
     * 模糊搜索用户（姓名/账号）
     */
    List<User> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 批量导入用户数据 - 用于 Admin 角色 R2 批量导入功能
     */
    int batchInsertUsers(@Param("userList") List<User> userList);

    /**
     * 分页查询用户
     */
    List<User> selectUserPage(
            @Param("pageSize") Integer pageSize,
            @Param("offset") Integer offset,
            @Param("roleId") Integer roleId,
            @Param("status") Integer status,
            @Param("keyword") String keyword
    );

    /**
     * 查询总数
     */
    Integer selectUserCount(
            @Param("roleId") Integer roleId,
            @Param("status") Integer status,
            @Param("keyword") String keyword
    );
}
