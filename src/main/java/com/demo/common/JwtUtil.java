package com.demo.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${app.jwt.secret:}")
    private String secret;

    @Value("${app.jwt.expire-hours:12}")
    private long expireHours;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (secret == null || secret.isBlank()) {
            byte[] bytes = new byte[48];
            new SecureRandom().nextBytes(bytes);
            String generated = Base64.getEncoder().encodeToString(bytes);
            log.warn("未配置 app.jwt.secret（APP_JWT_SECRET），已生成随机密钥；重启后旧令牌将失效。生产环境请务必配置固定密钥。");
            this.key = Keys.hmacShaKeyFor(generated.getBytes(StandardCharsets.UTF_8));
            return;
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, String username, int role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .addClaims(Map.of("username", username, "role", role))
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expireHours * 3600_000))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public int getRole(String token) {
        Object role = parse(token).get("role");
        return role instanceof Integer ? (Integer) role : Integer.parseInt(role.toString());
    }

    public String getUsername(String token) {
        return parse(token).get("username", String.class);
    }
}
