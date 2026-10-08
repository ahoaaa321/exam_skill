package com.demo.module.user.entity;

import java.time.LocalDateTime;

public record User(
        Long id,
        String username,
        String password,
        String realName,
        String idCard,
        String phone,
        String email,
        String recoveryToken,
        Integer gender,
        String workUnit,
        Integer age,
        String occupation,
        String incomeRange,
        String region,
        Integer role,
        Integer status,
        Integer mustChangePwd,
        Integer loginFailCount,
        LocalDateTime lockUntil,
        LocalDateTime lastLoginTime,
        String lastLoginIp,
        LocalDateTime createdAt
) {
    public String roleName() {
        return switch (role == null ? -1 : role) {
            case 0 -> "考生";
            case 1 -> "管理员";
            case 2 -> "考务人员";
            case 3 -> "超级管理员";
            default -> "未知";
        };
    }

    public String statusName() {
        return status != null && status == 1 ? "启用" : "禁用";
    }
}
