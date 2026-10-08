package com.demo.module.certificate.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.demo.module.certificate.repository.CertificateRepository;
import com.demo.module.score.repository.ScoreRepository;
import com.demo.module.system.repository.SystemRepository;
import com.demo.common.service.AuditService;
import com.demo.common.service.NotificationService;

@Service
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final ScoreRepository scoreRepository;
    private final SystemRepository systemRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public CertificateService(CertificateRepository certificateRepository, ScoreRepository scoreRepository,
                               SystemRepository systemRepository, NotificationService notificationService,
                               AuditService auditService) {
        this.certificateRepository = certificateRepository;
        this.scoreRepository = scoreRepository;
        this.systemRepository = systemRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    private String issuer() {
        return systemRepository.getConfig("certificate_issuer", "市职业技能等级认定中心");
    }

    /** 成绩合格后尝试发证；成功发放则通知考生 */
    public boolean issueIfPassed(Long registrationId) {
        int rows = certificateRepository.issueIfPassed(registrationId, issuer());
        if (rows > 0) {
            Long userId = scoreRepository.findUserIdByRegistration(registrationId);
            if (userId != null) {
                notificationService.notify(userId, "职业技能等级证书已生成",
                        "恭喜您考试合格，电子证书已生成，可在个人中心“我的证书”中查看与打印。");
            }
            auditService.log("CERT_ISSUE", String.valueOf(registrationId), "成绩合格自动发证");
            return true;
        }
        return false;
    }

    public List<Map<String, Object>> myCertificates(Long userId) {
        return certificateRepository.findByUserId(userId);
    }

    public Map<String, Object> verify(String certificateNo) {
        if (certificateNo == null || certificateNo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入证书编号");
        }
        return certificateRepository.findByCertificateNo(certificateNo.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未查询到该证书信息，请核对编号"));
    }

    public List<Map<String, Object>> all() {
        return certificateRepository.findAll();
    }

    public void revoke(Long id) {
        certificateRepository.revoke(id);
        auditService.log("CERT_REVOKE", String.valueOf(id), "证书作废处理");
    }

    /** 管理员手动发放证书 */
    public void issue(Long registrationId, String certificateNo, String tradeName, String levelName, String issuer) {
        certificateRepository.insert(registrationId, certificateNo, tradeName, levelName, issuer);
        Long userId = scoreRepository.findUserIdByRegistration(registrationId);
        if (userId != null) {
            notificationService.notify(userId, "职业技能等级证书已发放",
                    "您的【" + tradeName + " - " + levelName + "】证书已发放，证书编号：" + certificateNo + "，可在个人中心查看。");
        }
        auditService.log("CERT_ISSUE", String.valueOf(registrationId),
                "手动发放证书：" + certificateNo);
    }
}
