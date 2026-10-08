package com.demo.module.user.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.Result;
import com.demo.common.UserContext;
import com.demo.module.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        return Result.ok(userService.login(req.username(), req.password(), request));
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok(userService.register(req.username(), req.password(), req.name(), req.idCard(),
                req.phone(), req.email(), req.code()));
    }

    /** 发送注册邮箱验证码 */
    @PostMapping("/register/send-code")
    public Result<Map<String, Object>> sendRegisterCode(@Valid @RequestBody EmailRequest req) {
        return Result.ok(userService.sendRegisterVerifyCode(req.email()));
    }

    /** 发送找回密码邮箱验证码（公共邮箱必须同时提供用户名） */
    @PostMapping("/password/send-code")
    public Result<Map<String, Object>> sendResetCode(@Valid @RequestBody ResetCodeRequest req) {
        return Result.ok(userService.sendResetPasswordCode(req.email(), req.username()));
    }

    /** 邮箱验证码重置密码 */
    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPasswordByEmail(req.email(), req.code(), req.newPassword(), req.username());
        return Result.ok();
    }

    /** 恢复令牌重置密码 */
    @PostMapping("/password/reset-by-token")
    public Result<Void> resetPasswordByToken(@Valid @RequestBody ResetByTokenRequest req) {
        userService.resetPasswordByToken(req.token(), req.newPassword());
        return Result.ok();
    }

    /** 登录后修改密码 */
    @PostMapping("/password/change")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(UserContext.currentUserId(), req.oldPassword(), req.newPassword());
        return Result.ok();
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record RegisterRequest(
            @NotBlank String username,
            @NotBlank @Size(min = 8) String password,
            @NotBlank String name,
            @NotBlank String idCard,
            @NotBlank String phone,
            @NotBlank @Email String email,
            @NotBlank(message = "请输入邮箱验证码") String code
    ) {}
    public record EmailRequest(@NotBlank @Email String email) {}
    public record ResetCodeRequest(@NotBlank @Email String email, String username) {}
    public record ResetPasswordRequest(
            @NotBlank @Email String email,
            String username,
            @NotBlank String code,
            @NotBlank @Size(min = 8) String newPassword
    ) {}
    public record ResetByTokenRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8) String newPassword
    ) {}
    public record ChangePasswordRequest(@NotBlank String oldPassword, @NotBlank @Size(min = 8) String newPassword) {}
}
