package com.demo.module.system.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.demo.common.Result;
import com.demo.common.constant.SystemEmails;
import com.demo.common.security.RequireRole;
import com.demo.common.util.MaskUtils;
import com.demo.common.util.ValidationUtils;
import com.demo.module.user.entity.User;
import com.demo.module.user.repository.UserRepository;
import com.demo.common.service.AuditService;

/** 超级管理员对用户账号的管理。 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin
public class AdminUserController {

    private final UserRepository userRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public AdminUserController(UserRepository userRepository, AuditService auditService,
                               PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    @RequireRole({3})
    public Result<List<Map<String, Object>>> users() {
        return Result.ok(userRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.id());
            m.put("username", u.username());
            m.put("realName", u.realName());
            m.put("idCard", MaskUtils.maskMiddle(u.idCard()));
            m.put("phone", u.phone());
            m.put("email", u.email() == null ? "" : u.email());
            m.put("workUnit", u.workUnit() == null ? "" : u.workUnit());
            m.put("age", u.age());
            m.put("occupation", u.occupation() == null ? "" : u.occupation());
            m.put("incomeRange", u.incomeRange() == null ? "" : u.incomeRange());
            m.put("region", u.region() == null ? "" : u.region());
            m.put("role", u.role());
            m.put("roleName", u.roleName());
            m.put("status", u.status());
            m.put("statusName", u.statusName());
            m.put("lastLoginTime", u.lastLoginTime());
            return m;
        }).collect(Collectors.toList()));
    }

    @PostMapping("/users")
    @RequireRole({3})
    public Result<Void> createUser(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        if (userRepository.countByUsername(username) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        String idCard = (String) body.get("idCard");
        if (idCard != null && !idCard.isBlank()) {
            if (!ValidationUtils.isIdCard(idCard)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "身份证号必须为18位（末位可为X）");
            }
            if (userRepository.countByIdCard(idCard) > 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "该身份证号已被使用");
            }
        }
        String phone = (String) body.get("phone");
        if (phone != null && !phone.isBlank() && !ValidationUtils.isPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机号格式不正确");
        }
        String email = (String) body.get("email");
        if (email != null && !email.isBlank()) {
            if (!ValidationUtils.isEmail(email)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱格式不正确");
            }
            // 系统公共邮箱可多账号共用，其余邮箱禁止重复绑定
            if (!SystemEmails.isShared(email) && userRepository.findByEmail(email).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已被其他账号绑定");
            }
        }
        String pwd = (String) body.getOrDefault("password", "123456");
        User u = new User(null, username, passwordEncoder.encode(pwd),
                (String) body.get("realName"), idCard, (String) body.get("phone"),
                (String) body.get("email"), null, null, (String) body.get("workUnit"),
                null, null, null, null,
                body.get("role") == null ? 0 : ((Number) body.get("role")).intValue(), 1, 1, 0, null, null, null, null);
        userRepository.insert(u);
        auditService.log("USER_CREATE", username, "新建用户，角色 " + body.get("role"));
        return Result.ok();
    }

    @PutMapping("/users/{id}/status")
    @RequireRole({3})
    public Result<Void> toggleUserStatus(@PathVariable Long id, @RequestParam int status) {
        userRepository.updateStatus(id, status);
        auditService.log("USER_STATUS", String.valueOf(id), "账号状态调整为 " + status);
        return Result.ok();
    }

    @PutMapping("/users/{id}/role")
    @RequireRole({3})
    public Result<Void> updateUserRole(@PathVariable Long id, @RequestParam int role) {
        userRepository.updateRole(id, role);
        auditService.log("USER_ROLE", String.valueOf(id), "角色调整为 " + role);
        return Result.ok();
    }

    @PutMapping("/users/{id}/reset-password")
    @RequireRole({3})
    public Result<Void> resetUserPassword(@PathVariable Long id) {
        userRepository.resetPassword(id, passwordEncoder.encode("123456"));
        auditService.log("USER_RESET_PWD", String.valueOf(id), "重置用户密码");
        return Result.ok();
    }
}
