package com.group3.xd_lms.utils;

public class GetApiKey {

    // 从环境变量读取，这是最安全的方式
    private static final String SHOWAPI_APPKEY = System.getenv("SHOWAPI_APPKEY");

    // 也可以从JVM系统属性读取（-D参数传入）
    private static final String SHOWAPI_APPKEY_FROM_PROPERTY = System.getProperty("SHOWAPI_APPKEY");

    /**
     * 获取 ShowAPI 的 AppKey
     * 优先级：系统属性 > 环境变量
     */
    public static String getShowApiAppKey() {
        if (SHOWAPI_APPKEY_FROM_PROPERTY != null && !SHOWAPI_APPKEY_FROM_PROPERTY.isEmpty()) {
            return SHOWAPI_APPKEY_FROM_PROPERTY;
        }
        if (SHOWAPI_APPKEY != null && !SHOWAPI_APPKEY.isEmpty()) {
            return SHOWAPI_APPKEY;
        }
        throw new IllegalStateException("SHOWAPI_APPKEY 未配置！请在环境变量或 -D 参数中设置。");
    }
}