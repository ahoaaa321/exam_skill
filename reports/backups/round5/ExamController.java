package com.demo.module.exam.controller;

import com.demo.common.Result;
import com.demo.common.UserContext;
import com.demo.common.security.RequireRole;
import com.demo.module.exam.entity.ExamPlan;
import com.demo.module.exam.entity.Registration;
import com.demo.module.exam.service.ExamService;
import com.demo.module.exam.repository.PaymentRepository;
import com.demo.module.system.repository.SystemRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ExamController {

    private final ExamService examService;
    private final SystemRepository systemRepository;
    private final PaymentRepository paymentRepository;

    public ExamController(ExamService examService, SystemRepository systemRepository, PaymentRepository paymentRepository) {
        this.examService = examService;
        this.systemRepository = systemRepository;
        this.paymentRepository = paymentRepository;
    }

    // ============ 公开数据 ============
    @GetMapping("/public/plans")
    public Result<List<ExamPlan>> publicPlans() {
        return Result.ok(examService.publishedPlans());
    }

    @GetMapping("/public/announcements")
    public Result<List<Map<String, Object>>> announcements() {
        return Result.ok(systemRepository.announcements());
    }

    @GetMapping("/public/trades")
    public Result<List<Map<String, Object>>> trades() {
        return Result.ok(systemRepository.trades());
    }

    @GetMapping("/public/levels")
    public Result<List<Map<String, Object>>> levels() {
        return Result.ok(systemRepository.skillLevels());
    }

    // ============ 考试计划 ============
    @GetMapping("/plans")
    public Result<List<ExamPlan>> plans() {
        return Result.ok(examService.publishedPlans());
    }

    @GetMapping("/admin/plans")
    @RequireRole({1, 3})
    public Result<List<ExamPlan>> allPlans() {
        return Result.ok(examService.allPlans());
    }

    @PostMapping("/admin/plans")
    @RequireRole({1, 3})
    public Result<ExamPlan> createPlan(@Valid @RequestBody PlanRequest req) {
        return Result.ok(examService.createPlan(req.toPlan(), UserContext.currentUserId()));
    }

    @PutMapping("/admin/plans/{id}")
    @RequireRole({1, 3})
    public Result<ExamPlan> updatePlan(@PathVariable Long id, @Valid @RequestBody PlanRequest req) {
        return Result.ok(examService.updatePlan(id, req.toPlan()));
    }

    @DeleteMapping("/admin/plans/{id}")
    @RequireRole({1, 3})
    public Result<Void> deletePlan(@PathVariable Long id) {
        examService.deletePlan(id);
        return Result.ok();
    }

    @PutMapping("/admin/plans/{id}/status")
    @RequireRole({1, 3})
    public Result<ExamPlan> updatePlanStatus(@PathVariable Long id, @RequestParam int status) {
        return Result.ok(examService.updatePlanStatus(id, status));
    }

    // ============ 报名 ============
    @GetMapping("/registrations")
    @RequireRole({1, 2, 3})
    public Result<List<Registration>> registrations() {
        return Result.ok(examService.allRegistrations());
    }

    @GetMapping("/my/registrations")
    public Result<List<Registration>> myRegistrations() {
        return Result.ok(examService.myRegistrations(UserContext.currentUserId()));
    }

    @PostMapping("/registrations")
    public Result<Registration> apply(@Valid @RequestBody ApplyRequest req) {
        return Result.ok(examService.register(UserContext.currentUserId(), req.planId(),
                req.workYears(), req.education(), req.emergencyContact(), req.emergencyPhone()));
    }

    /** 考生本人取消报名（仅待审核/初审通过/审核退回可取消，释放名额） */
    @PutMapping("/my/registrations/{id}/cancel")
    public Result<Void> cancelMyRegistration(@PathVariable Long id) {
        examService.cancelRegistration(id, UserContext.currentUserId());
        return Result.ok();
    }

    // ============ 审核 ============
    @PutMapping("/registrations/{id}/audit")
    @RequireRole({1, 2, 3})
    public Result<Registration> audit(@PathVariable Long id, @RequestBody AuditRequest req) {
        return Result.ok(examService.audit(id, req.approved(), req.reason(), UserContext.currentUserId(), req.secondAudit() != null && req.secondAudit()));
    }

    // ============ 缴费 ============
    @PutMapping("/registrations/{id}/pay")
    public Result<Registration> pay(@PathVariable Long id,
                                    @RequestParam(required = false, defaultValue = "wechat") String payMethod) {
        return Result.ok(examService.pay(id, UserContext.currentUserId(), payMethod));
    }

    @PutMapping("/admin/registrations/{id}/confirm-pay")
    @RequireRole({1, 3})
    public Result<Registration> confirmPay(@PathVariable Long id) {
        return Result.ok(examService.confirmPay(id, UserContext.currentUserId()));
    }

    @GetMapping("/admin/payments")
    @RequireRole({1, 3})
    public Result<List<Map<String, Object>>> payments() {
        return Result.ok(paymentRepository.findAll());
    }

    // ============ 准考证 ============
    @GetMapping("/registrations/{id}/ticket")
    public Result<Map<String, Object>> ticket(@PathVariable Long id) {
        return Result.ok(examService.getTicket(id, UserContext.currentUserId(), UserContext.currentRole()));
    }

    // ============ 考场编排 ============
    @PostMapping("/arrangements")
    @RequireRole({2, 3})
    public Result<Map<String, Object>> arrange() {
        return Result.ok(examService.arrange());
    }

    @PostMapping("/arrangements/reset")
    @RequireRole({2, 3})
    public Result<Map<String, Object>> resetArrangements() {
        return Result.ok(examService.resetArrangements());
    }

    @GetMapping("/arrangements")
    @RequireRole({2, 3})
    public Result<List<Map<String, Object>>> arrangements() {
        return Result.ok(examService.arrangements());
    }

    @PutMapping("/arrangements/{id}")
    @RequireRole({2, 3})
    public Result<Void> updateArrangement(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object roomIdObj = body == null ? null : body.get("roomId");
        Object seatNoObj = body == null ? null : body.get("seatNo");
        if (!(roomIdObj instanceof Number) || !(seatNoObj instanceof Number)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "roomId 与 seatNo 不能为空且必须为数字");
        }
        Long roomId = ((Number) roomIdObj).longValue();
        int seatNo = ((Number) seatNoObj).intValue();
        examService.updateArrangement(id, roomId, seatNo);
        return Result.ok();
    }

    // ============ 统计 ============
    @GetMapping("/statistics")
    @RequireRole({1, 2, 3})
    public Result<Map<String, Object>> statistics() {
        return Result.ok(examService.statistics());
    }

    public record ApplyRequest(
            @NotNull Long planId,
            Integer workYears,
            String education,
            String emergencyContact,
            String emergencyPhone
    ) {}
    public record AuditRequest(boolean approved, String reason, Boolean secondAudit) {}

    public record PlanRequest(
            @NotNull String planName,
            @NotNull String planCode,
            @NotNull Long tradeId,
            @NotNull Long levelId,
            Long categoryId,
            @NotNull String registerStartTime,
            @NotNull String registerEndTime,
            @NotNull String examTime,
            String examLocation,
            @NotNull Integer maxCandidates,
            @NotNull java.math.BigDecimal fee,
            String conditionDesc
    ) {
        public ExamPlan toPlan() {
            return new ExamPlan(null, planName, planCode, tradeId, levelId, categoryId,
                    java.time.LocalDateTime.parse(registerStartTime),
                    java.time.LocalDateTime.parse(registerEndTime),
                    java.time.LocalDateTime.parse(examTime),
                    null, examLocation, maxCandidates, fee, conditionDesc,
                    0, 0, null, null, null, null, null);
        }
    }
}
