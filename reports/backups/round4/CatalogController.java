package com.demo.module.system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.Result;
import com.demo.common.UserContext;
import com.demo.common.security.RequireRole;
import com.demo.module.system.repository.SystemRepository;
import com.demo.common.service.AuditService;

/** 工种与技能等级基础数据管理。 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin
public class CatalogController {

    private final SystemRepository systemRepository;
    private final AuditService auditService;

    public CatalogController(SystemRepository systemRepository, AuditService auditService) {
        this.systemRepository = systemRepository;
        this.auditService = auditService;
    }

    // ============ 工种管理 ============
    @GetMapping("/trades")
    @RequireRole({1, 2, 3})
    public Result<List<Map<String, Object>>> allTrades() {
        return Result.ok(systemRepository.allTrades());
    }

    @PostMapping("/trades")
    @RequireRole({3})
    public Result<Void> createTrade(@RequestBody Map<String, Object> body) {
        systemRepository.insertTrade(
                (String) body.get("tradeName"),
                (String) body.get("tradeCode"),
                (String) body.get("tradeCategory"),
                (String) body.get("description"),
                body.get("sortOrder") == null ? 0 : ((Number) body.get("sortOrder")).intValue());
        auditService.log("TRADE_CREATE", (String) body.get("tradeCode"), "新增工种");
        return Result.ok();
    }

    @PutMapping("/trades/{id}")
    @RequireRole({3})
    public Result<Void> updateTrade(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        systemRepository.updateTrade(id,
                (String) body.get("tradeName"),
                (String) body.get("tradeCode"),
                (String) body.get("tradeCategory"),
                (String) body.get("description"),
                body.get("sortOrder") == null ? 0 : ((Number) body.get("sortOrder")).intValue());
        return Result.ok();
    }

    @DeleteMapping("/trades/{id}")
    @RequireRole({3})
    public Result<Void> deleteTrade(@PathVariable Long id) {
        systemRepository.deleteTrade(id);
        auditService.log("TRADE_DELETE", String.valueOf(id), "删除工种");
        return Result.ok();
    }

    @PutMapping("/trades/{id}/status")
    @RequireRole({3})
    public Result<Void> updateTradeStatus(@PathVariable Long id, @RequestParam int status) {
        systemRepository.updateTradeStatus(id, status);
        return Result.ok();
    }

    // ============ 等级管理 ============
    @GetMapping("/levels")
    @RequireRole({1, 2, 3})
    public Result<List<Map<String, Object>>> allLevels() {
        return Result.ok(systemRepository.allSkillLevels());
    }

    @PostMapping("/levels")
    @RequireRole({3})
    public Result<Void> createLevel(@RequestBody Map<String, Object> body) {
        systemRepository.insertSkillLevel(
                ((Number) body.get("tradeId")).longValue(),
                (String) body.get("levelName"),
                (String) body.get("levelCode"),
                body.get("levelRank") == null ? 0 : ((Number) body.get("levelRank")).intValue(),
                body.get("sortOrder") == null ? 0 : ((Number) body.get("sortOrder")).intValue());
        auditService.log("LEVEL_CREATE", (String) body.get("levelCode"), "新增技能等级");
        return Result.ok();
    }

    @PutMapping("/levels/{id}")
    @RequireRole({3})
    public Result<Void> updateLevel(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        systemRepository.updateSkillLevel(id,
                ((Number) body.get("tradeId")).longValue(),
                (String) body.get("levelName"),
                (String) body.get("levelCode"),
                body.get("levelRank") == null ? 0 : ((Number) body.get("levelRank")).intValue(),
                body.get("sortOrder") == null ? 0 : ((Number) body.get("sortOrder")).intValue());
        return Result.ok();
    }

    @DeleteMapping("/levels/{id}")
    @RequireRole({3})
    public Result<Void> deleteLevel(@PathVariable Long id) {
        systemRepository.deleteSkillLevel(id);
        auditService.log("LEVEL_DELETE", String.valueOf(id), "删除技能等级");
        return Result.ok();
    }

    @PutMapping("/levels/{id}/status")
    @RequireRole({3})
    public Result<Void> updateLevelStatus(@PathVariable Long id, @RequestParam int status) {
        systemRepository.updateSkillLevelStatus(id, status);
        return Result.ok();
    }
}
