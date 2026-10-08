package com.demo.module.exam.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.Result;
import com.demo.common.constant.UserRole;
import com.demo.common.security.RequireRole;
import com.demo.module.exam.repository.ArrangementConfigRepository;

/** 按考试计划维度的编排参数配置（考务）。URL 与原 ExtendedController 保持一致。 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class ArrangementConfigController {

    private final ArrangementConfigRepository configRepository;

    public ArrangementConfigController(ArrangementConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @GetMapping("/arrangements/config/{planId}")
    @RequireRole({UserRole.STAFF, UserRole.SUPER_ADMIN})
    public Result<Map<String, Object>> getConfig(@PathVariable Long planId) {
        return Result.ok(configRepository.findByPlan(planId));
    }

    @PutMapping("/arrangements/config/{planId}")
    @RequireRole({UserRole.STAFF, UserRole.SUPER_ADMIN})
    public Result<Void> saveConfig(@PathVariable Long planId, @RequestBody Map<String, Object> body) {
        int defaultSeatCount = body.get("defaultSeatCount") == null ? 30 : ((Number) body.get("defaultSeatCount")).intValue();
        int seatGap = body.get("seatGap") == null ? 1 : ((Number) body.get("seatGap")).intValue();
        int shuffleUnit = body.get("shuffleUnit") == null ? 1 : ((Number) body.get("shuffleUnit")).intValue();
        configRepository.upsert(planId, defaultSeatCount, seatGap, shuffleUnit);
        return Result.ok();
    }
}
