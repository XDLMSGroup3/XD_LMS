package com.group3.xd_lms.interceptor;

import com.alibaba.fastjson2.JSON;
import com.group3.xd_lms.utils.JwtUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        // 打印所有请求路径
        System.out.println("=== 拦截器请求 ===");
        System.out.println("路径: " + requestURI);
        System.out.println("方法: " + method);
        // 1. 从请求头中获取 token
        String token = request.getHeader("Authorization");

        // 2. 校验 token 是否存在
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> result = new HashMap<>();
            result.put("status", 401);
            result.put("message", "未登录，请先登录");
            response.getWriter().write(JSON.toJSONString(result));
            return false;
        }

        // 3. 去除 "Bearer " 前缀（如果存在）
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 4. 验证 token 是否有效
        try {
            // 解析 token 获取用户信息
            Map<String, Object> claims = JwtUtils.parseToken(token);

            // 将用户信息存入 request 属性，供后续接口使用
            request.setAttribute("userId", claims.get("userId"));
            request.setAttribute("userAccount", claims.get("userAccount"));
            request.setAttribute("roleId", claims.get("roleId"));

            return true;
        } catch (Exception e) {
            // token 无效或过期
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> result = new HashMap<>();
            result.put("status", 401);
            result.put("message", "Token无效或已过期，请重新登录");
            response.getWriter().write(JSON.toJSONString(result));
            return false;
        }
    }
}
