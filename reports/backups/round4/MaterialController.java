package com.demo.module.exam.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.Result;
import com.demo.common.UserContext;
import com.demo.common.constant.UserRole;
import com.demo.common.security.RequireRole;
import com.demo.module.exam.service.MaterialService;

/** 报名材料与审核进度。URL 与原 ExtendedController 保持一致。 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping("/registrations/{id}/materials")
    public Result<Void> addMaterial(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        materialService.addMaterial(id, (String) body.get("fileName"), (String) body.get("filePath"),
                ((Number) body.get("fileSize")).longValue(), (String) body.get("fileType"),
                UserContext.currentUserId(), UserContext.currentRole());
        return Result.ok();
    }

    @GetMapping("/registrations/{id}/materials")
    public Result<List<Map<String, Object>>> getMaterials(@PathVariable Long id) {
        return Result.ok(materialService.getMaterials(id, UserContext.currentUserId(), UserContext.currentRole()));
    }

    @DeleteMapping("/materials/{id}")
    public Result<Void> deleteMaterial(@PathVariable Long id) {
        materialService.deleteMaterial(id, UserContext.currentUserId(), UserContext.currentRole());
        return Result.ok();
    }

    @GetMapping("/registrations/{id}/audit-records")
    @RequireRole({UserRole.ADMIN, UserRole.STAFF, UserRole.SUPER_ADMIN})
    public Result<List<Map<String, Object>>> getAuditRecords(@PathVariable Long id) {
        return Result.ok(materialService.getAuditRecords(id));
    }

    @GetMapping("/registrations/{id}/timeline")
    public Result<List<Map<String, Object>>> getTimeline(@PathVariable Long id) {
        return Result.ok(materialService.getTimeline(id, UserContext.currentUserId(), UserContext.currentRole()));
    }
}
