package com.demo.config;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.demo.common.JwtUtil;
import com.demo.common.UserContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (isPublic(uri)) {
            chain.doFilter(request, response);
            return;
        }
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            unauthorized(response, "缺少登录凭证");
            return;
        }
        String token = header.substring(7);
        if (!jwtUtil.validate(token)) {
            unauthorized(response, "登录已过期，请重新登录");
            return;
        }
        try {
            Long userId = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);
            int role = jwtUtil.getRole(token);
            UserContext.set(new UserContext.CurrentUser(userId, username, role));
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private boolean isPublic(String uri) {
        if (uri.startsWith("/api/public/")) return true;
        if (uri.equals("/api/system/ai-qa")) return true;
        // 仅放行确需匿名的鉴权接口；登录后修改密码需鉴权，不放行
        if (uri.startsWith("/api/auth/")) {
            return uri.equals("/api/auth/login")
                    || uri.equals("/api/auth/register")
                    || uri.equals("/api/auth/register/send-code")
                    || uri.equals("/api/auth/password/send-code")
                    || uri.equals("/api/auth/password/reset")
                    || uri.equals("/api/auth/password/reset-by-token");
        }
        if (uri.startsWith("/assets/") || uri.startsWith("/css/") || uri.startsWith("/js/")) return true;
        if (uri.endsWith(".html") || uri.endsWith(".css") || uri.endsWith(".js") || uri.equals("/") || uri.equals("/favicon.ico")) return true;
        return false;
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }
}
