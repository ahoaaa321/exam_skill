package com.demo.module.score.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.demo.module.certificate.service.CertificateService;
import com.demo.module.score.entity.Score;
import com.demo.module.score.repository.ScoreRepository;
import com.demo.module.system.repository.SystemRepository;
import com.demo.common.service.AuditService;
import com.demo.common.service.NotificationService;

@Service
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final SystemRepository systemRepository;
    private final CertificateService certificateService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    private static final BigDecimal THEORY_WEIGHT = new BigDecimal("0.40");
    private static final BigDecimal PRACTICE_WEIGHT = new BigDecimal("0.60");

    public ScoreService(ScoreRepository scoreRepository, SystemRepository systemRepository,
                        CertificateService certificateService, NotificationService notificationService,
                        AuditService auditService) {
        this.scoreRepository = scoreRepository;
        this.systemRepository = systemRepository;
        this.certificateService = certificateService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    private BigDecimal passScore() {
        try {
            return new BigDecimal(systemRepository.getConfig("pass_score", "60"));
        } catch (Exception e) {
            return new BigDecimal("60");
        }
    }

    public List<Score> myScores(Long userId) {
        return scoreRepository.findByUserId(userId);
    }

    public List<Score> allScores() {
        return scoreRepository.findAll();
    }

    @Transactional
    public Score saveScore(Long registrationId, BigDecimal theory, BigDecimal practice, Long operatorId) {
        if (theory == null || practice == null) {
            throw new IllegalArgumentException("理论成绩与实操成绩不能为空");
        }
        if (theory.doubleValue() < 0 || theory.doubleValue() > 100
                || practice.doubleValue() < 0 || practice.doubleValue() > 100) {
            throw new IllegalArgumentException("成绩必须在 0-100 之间");
        }
        var old = scoreRepository.findByRegistrationId(registrationId);
        String oldSnapshot = old.map(s -> "理论" + s.theoryScore() + "，实操" + s.practiceScore()
                + "，综合" + s.comprehensiveScore()).orElse(null);

        scoreRepository.upsert(registrationId, theory, practice, THEORY_WEIGHT, PRACTICE_WEIGHT,
                passScore(), operatorId);
        Score saved = scoreRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new IllegalStateException("成绩保存失败"));

        if (oldSnapshot != null) {
            // 成绩更正留痕
            String newSnapshot = "理论" + saved.theoryScore() + "，实操" + saved.practiceScore()
                    + "，综合" + saved.comprehensiveScore();
            scoreRepository.insertChangeLog(saved.id(), oldSnapshot, newSnapshot, "成绩录入/更正", operatorId);
        }
        auditService.log("SCORE_SAVE", String.valueOf(registrationId),
                "录入成绩：理论" + theory + "，实操" + practice + "，综合" + saved.comprehensiveScore()
                        + (saved.result() != null && saved.result() == 1 ? "（合格）" : "（不合格）"));
        return saved;
    }

    @Transactional
    public Score publishScore(Long registrationId, Long operatorId) {
        int rows = scoreRepository.publish(registrationId);
        if (rows == 0) {
            // 成绩不存在或已发布：不存在抛 404/400，已发布则幂等拒绝，防止重复通知与重复发证
            boolean exists = scoreRepository.findByRegistrationId(registrationId).isPresent();
            if (!exists) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "成绩不存在，无法发布");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该成绩已发布，请勿重复操作");
        }
        Score score = scoreRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new IllegalStateException("成绩不存在，无法发布"));
        Long userId = scoreRepository.findUserIdByRegistration(registrationId);
        if (score != null && userId != null) {
            boolean pass = score.result() != null && score.result() == 1;
            notificationService.notify(userId,
                    pass ? "考试成绩已发布（合格）" : "考试成绩已发布（未合格）",
                    "您的【" + score.planName() + "】成绩：理论 " + score.theoryScore()
                            + "，实操 " + score.practiceScore() + "，综合 " + score.comprehensiveScore()
                            + "，结果：" + (pass ? "合格" : "不合格") + "。");
            if (pass) {
                certificateService.issueIfPassed(registrationId);
            }
        }
        auditService.log("SCORE_PUBLISH", String.valueOf(registrationId), "发布成绩");
        return score;
    }

    public Map<String, Object> resultStats() {
        return scoreRepository.resultStats();
    }

    public List<Map<String, Object>> changeLogs(Long registrationId) {
        return scoreRepository.findChangeLogs(registrationId);
    }
}
