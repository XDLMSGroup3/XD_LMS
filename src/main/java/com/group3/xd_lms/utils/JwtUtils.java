package com.group3.xd_lms.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtils {

    // 生成安全的密钥（替代原来的字符串密钥）
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // 或者使用固定密钥（需要 base64 编码）
    // private static final String SECRET_STRING = "your-256-bit-secret-key-here-must-be-32-chars";
    // private static final Key SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());

    private static final long EXPIRE_TIME = 86400000; // 1小时

    /**
     * 生成 token
     */
    public static String generateToken(Long userId, String userAccount, Integer roleId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("userAccount", userAccount);
        claims.put("roleId", roleId);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(SECRET_KEY)  // 新版本用法
                .compact();
    }

    /**
     * 解析 token
     */
    public static Map<String, Object> parseToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Map<String, Object> result = new HashMap<>();
        result.put("userId", claims.get("userId", Long.class));
        result.put("userAccount", claims.get("userAccount", String.class));
        result.put("roleId", claims.get("roleId", Integer.class));
        return result;
    }

    /**
     * 验证 token 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}