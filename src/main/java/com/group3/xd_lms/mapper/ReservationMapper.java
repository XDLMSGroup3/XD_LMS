package com.group3.xd_lms.mapper;

import com.group3.xd_lms.entity.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 图书预约映射接口 - 对应数据库 reservations 表
 * 处理读者预约申请及 R2 阶段 1 小时现场取书倒计时逻辑
 */
@Mapper
public interface ReservationMapper {

    /**
     * 提交图书预约申请 - 处理读者 R2 图书预约请求
     */
    int insertReservation(Reservation reservation);

    /**
     * 查询所有待取书记录 - 用于 R2 启动取书倒计时功能
     */
    List<Reservation> selectReadyReservations();

    /**
     * 更新预约状态 - 处理取书完成、超时自动释放、手动取消等逻辑
     */
    int updateReservationStatus(Reservation reservation);

    /**
     * 获取某书籍 ISBN 当前排队的第一位预约者
     */
    Reservation selectFirstInQueue(@Param("isbn") String isbn);
}