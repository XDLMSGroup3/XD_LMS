package com.group3.xd_lms.mapper;

import com.group3.xd_lms.entity.RenewalRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 续借申请映射接口 - 对应数据库 renewal_requests 表
 * 支持读者提交申请、查询历史及馆员审核逻辑
 */
@Mapper
public interface RenewalRequestMapper {

    /**
     * 提交新的续借申请 - 对应读者 R2 在线申请功能
     */
    int insert(RenewalRequest request);

    /**
     * 更新审批状态及馆员备注 - 对应馆员 R2 审批/驳回功能
     */
    int updateAuditStatus(RenewalRequest request);

    /**
     * 按状态获取申请列表 - 用于馆员待办列表展示
     */
    List<RenewalRequest> selectByStatus(@Param("status") String status);

    /**
     * 查询用户针对某次借阅是否存在“待处理”的申请 - 用于业务防重复校验
     */
    RenewalRequest selectPendingByUserAndBorrowId(@Param("userId") Integer userId, @Param("borrowId") Integer borrowId);

    /**
     * 根据主键查询
     */
    RenewalRequest selectById(@Param("id") Integer id);
}