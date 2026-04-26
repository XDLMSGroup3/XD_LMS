package com.group3.xd_lms.web;

import com.group3.xd_lms.entity.SystemSetting;
import com.group3.xd_lms.mapper.SystemSettingsMapper;
import com.group3.xd_lms.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
public class AdminConfigController {

    private final SystemSettingsMapper systemSettingsMapper;

    // 构造注入（和队友代码格式100%统一）
    public AdminConfigController(SystemSettingsMapper systemSettingsMapper) {
        this.systemSettingsMapper = systemSettingsMapper;
    }

    /**
     * 【接口1】查询所有全局配置（借阅天数、逾期罚金）
     */
    @GetMapping
    public HashMap<String, Object> getSystemConfig() {
        try {
            List<SystemSetting> allConfig = systemSettingsMapper.selectAllSettings();
            // 严格使用项目原生List返回格式
            return Result.getListResultMap(200, "全局配置查询成功", allConfig.size(), allConfig);
        } catch (Exception e) {
            return Result.getResultMap(500, "配置查询失败：" + e.getMessage());
        }
    }

    /**
     * 【接口2】管理员修改全局规则
     * 需求：配置借阅期限上限、每日逾期罚金金额
     */
    @PostMapping("/update")
    public HashMap<String, Object> updateConfig(@RequestBody Map<String, String> params) {
        // 接收前端传入的纯字符串参数
        String configKey = params.get("key");
        String configValueStr = params.get("value");

        // 基础非空校验
        if (configKey == null || configValueStr == null) {
            return Result.getResultMap(400, "配置项key、value不能为空");
        }

        try {
            // ===================== 核心修复：字符串转Float，匹配Mapper接口入参 =====================
            Float configValue;
            // 1. 借阅最大天数：必须为正整数，转Float兼容
            if ("max_loan_days".equals(configKey)) {
                int loanDays = Integer.parseInt(configValueStr);
                if (loanDays <= 0) {
                    return Result.getResultMap(400, "借阅期限天数必须大于0");
                }
                configValue = (float) loanDays;
            }
            // 2. 每日逾期罚金：不能为负数，直接转Float
            else if ("fine_per_day".equals(configKey)) {
                configValue = Float.parseFloat(configValueStr);
                if (configValue < 0) {
                    return Result.getResultMap(400, "每日逾期罚金金额不能为负数");
                }
            }
            // 其他配置项通用转换
            else {
                configValue = Float.parseFloat(configValueStr);
            }

            // ===================== 调用Mapper：现在传入Float类型，和接口签名100%匹配 =====================
            int updateRows = systemSettingsMapper.updateSetting(configKey, configValue);

            if (updateRows > 0) {
                return Result.getResultMap(200, "全局规则配置更新成功");
            } else {
                return Result.getResultMap(400, "配置更新失败，无对应配置项");
            }

        } catch (NumberFormatException e) {
            // 数字格式异常兜底
            return Result.getResultMap(400, "配置数值格式错误，请输入合法数字");
        } catch (Exception e) {
            // 全局异常兜底
            return Result.getResultMap(500, "服务器异常：" + e.getMessage());
        }
    }
}