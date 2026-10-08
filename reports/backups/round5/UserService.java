package com.demo.module.user.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.demo.common.JwtUtil;
import com.demo.common.constant.SystemEmails;
import com.demo.common.util.ValidationUtils;
import com.demo.module.user.entity.User;
import com.demo.module.user.repository.UserRepository;
import com.demo.common.service.AuditService;
import com.demo.common.service.EmailService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_FAIL = 5;
    private static final int LOCK_MINUTES = 30;

    public UserService(UserRepository userRepository, JwtUtil jwtUtil, EmailService emailService,
                       AuditService auditService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    public Map<String, Object> login(String username, String password, HttpServletRequest request) {
        String ip = getClientIp(request);
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            auditService.loginLog(null, username, false, "账号不存在", ip);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        if (user.status() == 0) {
            auditService.loginLog(user.id(), username, false, "账号已禁用", ip);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已被禁用");
        }
        if (user.lockUntil() != null && user.lockUntil().isAfter(LocalDateTime.now())) {
            auditService.loginLog(user.id(), username, false, "账号锁定中", ip);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已锁定，请30分钟后重试");
        }
        if (!passwordEncoder.matches(password, user.password())) {
            int failCount = user.loginFailCount() + 1;
            LocalDateTime lockUntil = failCount >= MAX_FAIL ? LocalDateTime.now().plusMinutes(LOCK_MINUTES) : null;
            userRepository.incrementFailCount(user.id(), failCount, lockUntil);
            auditService.loginLog(user.id(), username, false, "密码错误第" + failCount + "次", ip);
            if (failCount >= MAX_FAIL) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "密码错误次数过多，账号已锁定30分钟");
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误，剩余尝试次数：" + (MAX_FAIL - failCount));
        }
        userRepository.updateLoginInfo(user.id(), LocalDateTime.now(), ip);
        auditService.loginLog(user.id(), username, true, null, ip);
        String token = jwtUtil.generate(user.id(), user.username(), user.role());
        return Map.of(
                "token", token,
                "userId", user.id(),
                "username", user.username(),
                "name", user.realName(),
                "role", user.role(),
                "roleName", user.roleName(),
                "mustChangePwd", user.mustChangePwd() == 1
        );
    }

    public Map<String, Object> register(String username, String password, String name, String idCard,
                                        String phone, String email, String code) {
        if (userRepository.countByUsername(username) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        if (userRepository.countByIdCard(idCard) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该身份证已注册");
        }
        // 系统公共邮箱允许被多个账号共用，其余邮箱保持唯一
        if (!SystemEmails.isShared(email) && userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已被注册");
        }
        if (!emailService.verifyCode(email, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱验证码错误或已过期");
        }
        if (!ValidationUtils.isPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机号格式不正确");
        }
        if (!ValidationUtils.isIdCard(idCard)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "身份证号必须为18位（末位可为X）");
        }
        if (!ValidationUtils.isStrongPassword(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码至少8位且需包含字母和数字");
        }
        String recoveryToken = UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        User user = new User(null, username, passwordEncoder.encode(password), name, idCard, phone, email, recoveryToken, null, null, null, null, null, null, 0, 1, 0, 0, null, null, null, null);
        userRepository.insert(user);
        auditService.log("REGISTER", username, "新用户注册：" + name);
        return Map.of("recoveryToken", recoveryToken, "username", username);
    }

    public Map<String, Object> sendRegisterVerifyCode(String email) {
        // 系统公共邮箱允许多账号注册，可重复发送验证码；其余邮箱已注册则拦截
        if (!SystemEmails.isShared(email) && userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已被注册");
        }
        return emailService.sendVerifyCode(email, "账号注册");
    }

    public Map<String, Object> sendResetPasswordCode(String email) {
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "该邮箱未注册"));
        return emailService.sendVerifyCode(email, "找回密码");
    }

    public void resetPasswordByEmail(String email, String code, String newPassword) {
        if (!emailService.verifyCode(email, code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码错误或已过期");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "该邮箱未注册"));
        validatePassword(newPassword);
        userRepository.updatePassword(user.id(), passwordEncoder.encode(newPassword));
    }

    public void resetPasswordByToken(String token, String newPassword) {
        User user = userRepository.findByRecoveryToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "恢复令牌无效"));
        validatePassword(newPassword);
        userRepository.updatePassword(user.id(), passwordEncoder.encode(newPassword));
        userRepository.clearRecoveryToken(user.id());
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        if (!passwordEncoder.matches(oldPassword, user.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "原密码错误");
        }
        if (!ValidationUtils.isStrongPassword(newPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码至少8位且需包含字母和数字");
        }
        userRepository.updatePassword(userId, passwordEncoder.encode(newPassword));
    }

    private void validatePassword(String password) {
        if (!ValidationUtils.isStrongPassword(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码至少8位且需包含字母和数字");
        }
    }

    private String getClientIp(HttpServletRequest request) {
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
    }
}
