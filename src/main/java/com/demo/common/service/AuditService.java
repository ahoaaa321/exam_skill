package com.demo.common.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.demo.common.UserContext;
import com.demo.module.system.repository.SystemRepository;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 操作审计 / 登录日志服务。
 * 关键业务动作统一在此落库，供系统端审计追溯。
 */
@Service
public class AuditService {

    private final SystemRepository systemRepository;
    private final JdbcTemplate jdbc;

    public AuditService(SystemRepository systemRepository, JdbcTemplate jdbc) {
        this.systemRepository = systemRepository;
        this.jdbc = jdbc;
    }

    /** 记录当前登录用户的操作（自动取操作人、IP） */
    public void log(String type, String target, String detail) {
        try {
            var user = UserContext.get();
            Long operatorId = user == null ? null : user.id();
            String operatorName = user == null ? "system" : user.username();
            systemRepository.insertLog(operatorId, operatorName, type, target, detail, currentIp());
        } catch (Exception ignored) {
            // 审计失败不应阻断主业务
        }
    }

    /** 记录带操作前/后快照的操作日志 */
    public void logWithSnapshot(String type, String target, String detail, String beforeValue, String afterValue) {
        try {
            var user = UserContext.get();
            Long operatorId = user == null ? null : user.id();
            String operatorName = user == null ? "system" : user.username();
            systemRepository.insertLogWithSnapshot(operatorId, operatorName, type, target, detail,
                    beforeValue, afterValue, currentIp());
        } catch (Exception ignored) {
        }
    }

    /** 登录日志（成功/失败） */
    public void loginLog(Long userId, String username, boolean success, String reason, String ip) {
        try {
            jdbc.update(
                    "INSERT INTO sys_login_log (user_id, username, login_type, login_result, client_ip, fail_reason) VALUES (?,?,1,?,?,?)",
                    userId, username, success ? 1 : 0, ip, reason);
        } catch (Exception ignored) {
        }
    }

    public static String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }
}
