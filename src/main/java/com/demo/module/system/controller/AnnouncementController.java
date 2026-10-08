package com.demo.module.system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
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
import org.springframework.web.server.ResponseStatusException;

import com.demo.common.Result;
import com.demo.common.UserContext;
import com.demo.common.security.RequireRole;
import com.demo.module.system.repository.SystemRepository;
import com.demo.common.service.AuditService;

/** 公告管理。 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin
public class AnnouncementController {

    private final SystemRepository systemRepository;
    private final AuditService auditService;

    public AnnouncementController(SystemRepository systemRepository, AuditService auditService) {
        this.systemRepository = systemRepository;
        this.auditService = auditService;
    }

    @GetMapping("/announcements")
    @RequireRole({1, 2, 3})
    public Result<List<Map<String, Object>>> allAnnouncements() {
        return Result.ok(systemRepository.allAnnouncements());
    }

    @PostMapping("/announcements")
    @RequireRole({1, 3})
    public Result<Void> createAnnouncement(@RequestBody Map<String, Object> body) {
        systemRepository.insertAnnouncement(
                (String) body.get("title"),
                (String) body.get("content"),
                body.get("isTop") == null ? 0 : ((Number) body.get("isTop")).intValue(),
                UserContext.currentUserId());
        auditService.log("ANNOUNCE_CREATE", (String) body.get("title"), "发布公告");
        return Result.ok();
    }

    @PutMapping("/announcements/{id}/status")
    @RequireRole({1, 3})
    public Result<Void> updateAnnouncementStatus(@PathVariable Long id, @RequestParam int status) {
        systemRepository.updateAnnouncementStatus(id, status);
        auditService.logWithSnapshot("ANNOUNCE_STATUS", String.valueOf(id), "公告状态调整为 " + status,
                null, "status=" + status);
        return Result.ok();
    }

    @DeleteMapping("/announcements/{id}")
    @RequireRole({1, 3})
    public Result<Void> deleteAnnouncement(@PathVariable Long id) {
        systemRepository.deleteAnnouncement(id);
        auditService.log("ANNOUNCE_DELETE", String.valueOf(id), "删除公告");
        return Result.ok();
    }

    @PostMapping("/announcements/import")
    @RequireRole({1, 3})
    public Result<Map<String, Object>> importAnnouncements(@RequestBody Map<String, Object> body) {
        Object itemsObj = body.get("items");
        if (itemsObj == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "items 字段不能为空");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> list = (List<Map<String, Object>>) itemsObj;
        if (list.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "导入数据不能为空");
        }
        int success = 0;
        for (Map<String, Object> item : list) {
            String title = (String) item.get("title");
            String content = (String) item.get("content");
            if (title == null || title.isBlank()) continue;
            int isTop = item.get("isTop") == null ? 0 : ((Number) item.get("isTop")).intValue();
            systemRepository.insertAnnouncement(title, content == null ? "" : content, isTop, UserContext.currentUserId());
            success++;
        }
        auditService.log("ANNOUNCE_IMPORT", "batch", "批量导入公告 " + success + " 条");
        return Result.ok(Map.of("imported", success, "total", list.size()));
    }
}
