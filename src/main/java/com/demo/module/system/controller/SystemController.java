package com.demo.module.system.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.Result;
import com.demo.common.security.RequireRole;
import com.demo.module.system.repository.SystemRepository;
import com.demo.common.service.AuditService;

/** 系统参数、运行日志、统计与健康检查。 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin
public class SystemController {

    private final SystemRepository systemRepository;
    private final AuditService auditService;

    public SystemController(SystemRepository systemRepository, AuditService auditService) {
        this.systemRepository = systemRepository;
        this.auditService = auditService;
    }

    @GetMapping("/configs")
    @RequireRole({1, 2, 3})
    public Result<List<Map<String, Object>>> configs() {
        return Result.ok(systemRepository.configs());
    }

    @PutMapping("/configs")
    @RequireRole({3})
    public Result<Void> updateConfig(@RequestBody Map<String, String> body) {
        systemRepository.updateConfig(body.get("key"), body.get("value"));
        auditService.log("CONFIG_UPDATE", body.get("key"), "更新参数：" + body.get("value"));
        return Result.ok();
    }

    @GetMapping("/logs")
    @RequireRole({3})
    public Result<List<Map<String, Object>>> logs(
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "100") int limit) {
        return Result.ok(systemRepository.operationLogs(operator, type, limit));
    }

    @GetMapping("/login-logs")
    @RequireRole({3})
    public Result<List<Map<String, Object>>> loginLogs(@RequestParam(defaultValue = "100") int limit) {
        return Result.ok(systemRepository.loginLogs(limit));
    }

    // ============ 用户注册统计（按年月） ============
    @GetMapping("/stats/user-registration")
    @RequireRole({1, 2, 3})
    public Result<List<Map<String, Object>>> userRegistrationMonthly(@RequestParam int year) {
        return Result.ok(systemRepository.userRegistrationMonthly(year));
    }

    // ============ 全平台报考分类汇总 ============
    @GetMapping("/stats/registration-category")
    @RequireRole({1, 2, 3})
    public Result<List<Map<String, Object>>> registrationCategorySummary() {
        return Result.ok(systemRepository.registrationCategorySummary());
    }

    @GetMapping("/database-health")
    @RequireRole({1, 2, 3})
    public Result<Map<String, Object>> databaseHealth() {
        boolean ok = systemRepository.ping();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("connected", ok);
        map.put("database", "skill_exam");
        map.put("checkedAt", java.time.LocalDateTime.now().toString());
        return Result.ok(map);
    }
}
