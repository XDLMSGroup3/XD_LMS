package com.group3.xd_lms.mapper;

import com.group3.xd_lms.entity.SystemSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 系统配置映射接口 - 对应数据库 system_settings 表
 * 支持管理员 R2 配置借阅期、逾期金额等全局业务规则
 */
@Mapper
public interface SystemSettingsMapper {

    /**
     * 获取所有业务规则 - 对应管理员 R2 配置全局规则功能
     */
    List<SystemSetting> selectAllSettings();

    /**
     * 根据配置键获取具体配置值（如借阅天数）
     */
    BigDecimal selectValueByKey(@Param("key") String key);

    /**
     * 更新业务配置值 - 支持管理员 R2 修改核心规则
     */
    int updateSetting(@Param("key") String key, @Param("value") Float value);
}
