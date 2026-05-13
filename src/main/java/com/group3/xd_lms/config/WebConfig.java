package com.group3.xd_lms.config;

import com.group3.xd_lms.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")                    // 拦截所有请求
                .excludePathPatterns(
                        "/**/users/login",                    // 登录接口不拦截
                        "/**/users/register",                 // 注册接口不拦截
                        "/swagger-resources/**",               // Swagger 文档
                        "/webjars/**",
                        "/v2/api-docs",
                        "/doc.html",
                        "/error"                                // 错误页面
                );
    }
}
