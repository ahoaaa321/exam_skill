package com.demo.module.system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
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

/** 售后服务工单。 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin
public class AfterSaleController {

    private final SystemRepository systemRepository;
    private final AuditService auditService;

    public AfterSaleController(SystemRepository systemRepository, AuditService auditService) {
        this.systemRepository = systemRepository;
        this.auditService = auditService;
    }

    @GetMapping("/after-sales")
    public Result<List<Map<String, Object>>> myAfterSalesTickets() {
        return Result.ok(systemRepository.afterSalesTickets(UserContext.currentUserId()));
    }

    @GetMapping("/after-sales/all")
    @RequireRole({1, 3})
    public Result<List<Map<String, Object>>> allAfterSalesTickets() {
        return Result.ok(systemRepository.afterSalesTickets(null));
    }

    @PostMapping("/after-sales")
    public Result<Void> createAfterSalesTicket(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        if (title == null || title.isBlank() || content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题和内容不能为空");
        }
        Long registrationId = null;
        if (body.get("registrationId") != null) {
            if (!(body.get("registrationId") instanceof Number regIdNum)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "registrationId 必须为数字");
            }
            registrationId = regIdNum.longValue();
            // 归属校验：报名记录必须存在且属于当前登录用户，防止横向越权
            Long ownerId = systemRepository.findRegistrationOwnerId(registrationId);
            if (ownerId == null || !ownerId.equals(UserContext.currentUserId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "报名记录不存在或不属于本人");
            }
        }
        systemRepository.insertAfterSalesTicket(UserContext.currentUserId(), registrationId, title, content,
                (String) body.get("type"));
        auditService.log("AFTER_SALES_CREATE", title, "提交售后服务工单");
        return Result.ok();
    }

    @PutMapping("/after-sales/{id}/reply")
    @RequireRole({1, 3})
    public Result<Void> replyAfterSalesTicket(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reply = body.get("reply");
        if (reply == null || reply.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "回复内容不能为空");
        }
        systemRepository.replyAfterSalesTicket(id, reply, UserContext.currentUserId());
        auditService.log("AFTER_SALES_REPLY", String.valueOf(id), "回复售后工单");
        return Result.ok();
    }

    @PutMapping("/after-sales/{id}/status")
    @RequireRole({1, 3})
    public Result<Void> updateAfterSalesStatus(@PathVariable Long id, @RequestParam int status) {
        if (status < 1 || status > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "工单状态仅允许 1（处理中）、2（已解决）、3（已关闭）");
        }
        systemRepository.updateAfterSalesStatus(id, status);
        auditService.log("AFTER_SALES_STATUS", String.valueOf(id), "工单状态调整为 " + status);
        return Result.ok();
    }
}
