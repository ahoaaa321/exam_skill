package com.demo.module.system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.Result;
import com.demo.common.constant.UserRole;
import com.demo.common.security.RequireRole;
import com.demo.module.system.repository.SystemRepository;

/** 登录日志查询（超管）。URL 与原 ExtendedController 保持一致。 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class LoginLogController {

    private final SystemRepository systemRepository;

    public LoginLogController(SystemRepository systemRepository) {
        this.systemRepository = systemRepository;
    }

    @GetMapping("/admin/login-logs")
    @RequireRole({UserRole.SUPER_ADMIN})
    public Result<List<Map<String, Object>>> loginLogs(@RequestParam(required = false) String username,
                                                       @RequestParam(required = false) Integer result) {
        return Result.ok(systemRepository.loginLogs(username, result));
    }
}
