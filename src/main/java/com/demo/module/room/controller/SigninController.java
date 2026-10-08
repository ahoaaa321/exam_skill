package com.demo.module.room.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.demo.common.Result;
import com.demo.common.UserContext;
import com.demo.common.constant.UserRole;
import com.demo.common.security.RequireRole;
import com.demo.module.room.repository.SigninRepository;

/** 考试日签到管理（考务）。URL 与原 ExtendedController 保持一致。 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class SigninController {

    private final SigninRepository signinRepository;

    public SigninController(SigninRepository signinRepository) {
        this.signinRepository = signinRepository;
    }

    @GetMapping("/signins")
    @RequireRole({UserRole.STAFF, UserRole.SUPER_ADMIN})
    public Result<List<Map<String, Object>>> getSignins() {
        return Result.ok(signinRepository.findAll());
    }

    @PutMapping("/signins/{arrangementId}/signin")
    @RequireRole({UserRole.STAFF, UserRole.SUPER_ADMIN})
    public Result<Void> signin(@PathVariable Long arrangementId,
                               @RequestParam(defaultValue = "1") int type) {
        signinRepository.signin(arrangementId, type);
        return Result.ok();
    }

    @PutMapping("/signins/{arrangementId}/absent")
    @RequireRole({UserRole.STAFF, UserRole.SUPER_ADMIN})
    public Result<Void> markAbsent(@PathVariable Long arrangementId) {
        signinRepository.markAbsent(arrangementId);
        return Result.ok();
    }

    /** 考生查看本人的签到/考场编排信息（任意登录用户，数据按本人 user_id 过滤） */
    @GetMapping("/my/signins")
    public Result<List<Map<String, Object>>> mySignins() {
        return Result.ok(signinRepository.findByUserId(UserContext.currentUserId()));
    }

    /** 考生自助签到：只能对本人报名对应的编排签到，不能替他人操作 */
    @PutMapping("/my/signins/{arrangementId}/signin")
    public Result<Void> mySignin(@PathVariable Long arrangementId) {
        int rows = signinRepository.signinForUser(arrangementId, UserContext.currentUserId(), 1);
        if (rows == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "签到记录不存在或不属于当前考生");
        }
        return Result.ok();
    }
}
