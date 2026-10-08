package com.demo.module.exam.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.demo.common.Result;
import com.demo.common.constant.UserRole;
import com.demo.common.security.RequireRole;
import com.demo.module.exam.repository.InvigilatorRepository;

/** 监考人员维护（考务）。URL 与原 ExtendedController 保持一致。 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class InvigilatorController {

    private final InvigilatorRepository invigilatorRepository;

    public InvigilatorController(InvigilatorRepository invigilatorRepository) {
        this.invigilatorRepository = invigilatorRepository;
    }

    @GetMapping("/invigilators")
    @RequireRole({UserRole.STAFF, UserRole.SUPER_ADMIN})
    public Result<List<Map<String, Object>>> getInvigilators() {
        return Result.ok(invigilatorRepository.findAll());
    }

    @PostMapping("/invigilators")
    @RequireRole({UserRole.STAFF, UserRole.SUPER_ADMIN})
    public Result<Void> addInvigilator(@RequestBody Map<String, Object> body) {
        if (!(body.get("name") instanceof String name) || name.isBlank()
                || !(body.get("phone") instanceof String phone) || phone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name 和 phone 必须为非空字符串");
        }
        invigilatorRepository.insert(name, phone);
        return Result.ok();
    }

    @DeleteMapping("/invigilators/{id}")
    @RequireRole({UserRole.STAFF, UserRole.SUPER_ADMIN})
    public Result<Void> deleteInvigilator(@PathVariable Long id) {
        invigilatorRepository.delete(id);
        return Result.ok();
    }
}
