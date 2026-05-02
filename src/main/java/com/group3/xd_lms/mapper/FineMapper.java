package com.group3.xd_lms.mapper;

import com.group3.xd_lms.dto.UnpaidFineDetailDTO;
import com.group3.xd_lms.entity.Fine;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.util.List;

/**
 * 罚款记录映射接口 - 对应数据库 fines 表
 * 支持读者查询罚款明细及管理员处理欠款
 */
@Mapper
public interface FineMapper {

    /**
     * 查询个人罚款明细 - 对应读者 R2 查询逾期费用功能
     */
    List<Fine> selectFinesByUserId(@Param("userId") Long userId);

    /**
     * 查询个人未支付罚款明细 - 包含关联的借阅记录和书籍信息
     * 对应读者 R2 查询逾期费用功能，提供更丰富的罚款上下文
     */
    List<UnpaidFineDetailDTO> selectUnpaidFinesWithDetails(@Param("userId") Long userId);

    /**
     * 产生逾期罚款记录 - 系统扫描逾期后自动生成
     */
    int insertFine(Fine fine);

    /**
     * 更新支付状态 - 处理读者线下/线上缴费后状态变更
     */
    int updatePaymentStatus(@Param("fineId") Long fineId, @Param("status") String status);

    /**
     * 汇总用户所有未支付罚款总额
     */
    BigDecimal sumUnpaidFinesByUserId(@Param("userId") Long userId);


    /**
     * 每日凌晨自动计算逾期金额
     */
    int incrementDailyFines(@Param("dailyFineAmount") BigDecimal dailyFineAmount);

    /**
     * 根据借阅记录ID查询罚款单，用于去重校验
     */
    Fine selectByBorrowRecordId(@Param("borrowRecordId") Long borrowRecordId);

    /**
     * 更新罚款金额（直接覆盖，不累加）
     * @param id     罚款记录 ID
     * @param amount 新的罚款金额
     * @return 更新的行数
     */
    int updateFineAmount(@Param("id") Long id, @Param("amount") BigDecimal amount);

}
