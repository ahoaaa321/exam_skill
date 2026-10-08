package com.demo.module.system.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.demo.common.Result;
import com.demo.common.UserContext;
import com.demo.common.constant.SystemEmails;
import com.demo.common.util.MaskUtils;
import com.demo.common.util.ValidationUtils;
import com.demo.module.user.entity.User;
import com.demo.module.user.repository.UserRepository;

/** 当前登录用户的个人信息。 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin
public class ProfileController {

    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        User u = userRepository.findById(UserContext.currentUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.id());
        map.put("username", u.username());
        map.put("name", u.realName());
        map.put("role", u.role());
        map.put("roleName", u.roleName());
        map.put("phone", u.phone());
        map.put("email", u.email() == null ? "" : u.email());
        map.put("idCard", MaskUtils.maskMiddle(u.idCard()));
        map.put("workUnit", u.workUnit() == null ? "" : u.workUnit());
        map.put("age", u.age());
        map.put("occupation", u.occupation() == null ? "" : u.occupation());
        map.put("incomeRange", u.incomeRange() == null ? "" : u.incomeRange());
        map.put("region", u.region() == null ? "" : u.region());
        map.put("status", u.status());
        map.put("lastLoginTime", u.lastLoginTime());
        map.put("lastLoginIp", u.lastLoginIp());
        map.put("systemEmail", SystemEmails.isShared(u.email()));
        return Result.ok(map);
    }

    @PutMapping("/me/profile")
    public Result<Void> updateProfile(@RequestBody Map<String, Object> body) {
        String phone = (String) body.get("phone");
        if (phone != null && !phone.isBlank() && !ValidationUtils.isPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "手机号格式不正确");
        }
        String email = (String) body.get("email");
        if (email != null && !email.isBlank()) {
            if (!ValidationUtils.isEmail(email)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱格式不正确");
            }
            // 系统公共邮箱可多账号共用；其余邮箱不得与其他账号重复
            Long currentUserId = UserContext.currentUserId();
            if (!SystemEmails.isShared(email)) {
                userRepository.findByEmail(email).ifPresent(owner -> {
                    if (!owner.id().equals(currentUserId)) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已被其他账号绑定");
                    }
                });
            }
        }
        userRepository.updateProfile(UserContext.currentUserId(),
                (String) body.get("realName"), phone, email, (String) body.get("workUnit"));

        // 扩展资料：年龄、职业、收入范围、地区
        Object ageObj = body.get("age");
        Integer age = null;
        if (ageObj != null && !ageObj.toString().isBlank()) {
            try {
                age = Integer.parseInt(ageObj.toString());
                if (age < 0 || age > 150) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "年龄不合法");
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "年龄必须为数字");
            }
        }
        userRepository.updateProfileExtended(UserContext.currentUserId(), age,
                (String) body.get("occupation"), (String) body.get("incomeRange"), (String) body.get("region"));
        return Result.ok();
    }
}
