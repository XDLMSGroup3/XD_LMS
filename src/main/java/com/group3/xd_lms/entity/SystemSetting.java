package com.group3.xd_lms.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 系统全局配置实体类 - 对应数据库 system_settings 表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSetting {

    /**
     * 配置键（主键），例如：max_loan_days
     */
    @NotBlank(message = "配置键不能为空")
    private String settingKey;

    /**
     * 配置值
     */
    @NotBlank(message = "配置值不能为空")
    private Float settingValue;

    /**
     * 配置描述
     */
    private String description;
}
