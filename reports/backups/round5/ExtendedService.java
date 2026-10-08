package com.demo.module.exam.service;

import com.demo.module.exam.repository.ExtendedRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ExtendedService {

    private final ExtendedRepository repo;

    public ExtendedService(ExtendedRepository repo) {
        this.repo = repo;
    }

    // 材料
    public void addMaterial(Long registrationId, String fileName, String filePath, long fileSize, String fileType,
                            Long currentUserId, int currentRole) {
        ensureMaterialAccess(registrationId, currentUserId, currentRole);
        repo.insertMaterial(registrationId, fileName, filePath, fileSize, fileType);
    }

    public List<Map<String, Object>> getMaterials(Long registrationId, Long currentUserId, int currentRole) {
        ensureMaterialAccess(registrationId, currentUserId, currentRole);
        return repo.findMaterialsByRegistration(registrationId);
    }

    public void deleteMaterial(Long id, Long currentUserId, int currentRole) {
        Long regId = repo.findRegistrationIdByMaterial(id);
        if (regId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "材料不存在");
        }
        ensureMaterialAccess(regId, currentUserId, currentRole);
        repo.deleteMaterial(id);
    }

    /** 材料归属校验：本人或管理/考务/超管可操作 */
    private void ensureMaterialAccess(Long registrationId, Long currentUserId, int currentRole) {
        Long ownerId = repo.findRegistrationUserId(registrationId);
        if (ownerId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "报名记录不存在");
        }
        if (currentRole == 0 && !ownerId.equals(currentUserId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "无权操作他人报名材料");
        }
    }

    // 审核记录
    public void addAuditRecord(Long registrationId, Long auditorId, int auditLevel, int auditResult, String reason) {
        repo.insertAuditRecord(registrationId, auditorId, auditLevel, auditResult, reason);
    }

    public List<Map<String, Object>> getAuditRecords(Long registrationId) {
        return repo.findAuditRecords(registrationId);
    }

    /** 考生查看本人报名审核进度（含归属权校验） */
    public List<Map<String, Object>> getTimeline(Long registrationId, Long currentUserId, int currentRole) {
        ensureMaterialAccess(registrationId, currentUserId, currentRole);
        return repo.findAuditRecords(registrationId);
    }

    // 支付
    public void addPayment(Long registrationId, java.math.BigDecimal amount, String payMethod) {
        repo.insertPayment(registrationId, amount, payMethod);
    }

    public List<Map<String, Object>> getPayments(Long registrationId) {
        return repo.findPaymentsByRegistration(registrationId);
    }

    public List<Map<String, Object>> getAllPayments() {
        return repo.findAllPayments();
    }

    // 消息
    public void sendMessage(Long userId, String title, String content) {
        repo.insertMessage(userId, title, content);
    }

    public List<Map<String, Object>> getMessages(Long userId) {
        return repo.findMessagesByUser(userId);
    }

    public void markRead(Long id) {
        repo.markMessageRead(id);
    }

    public void markAllRead(Long userId) {
        repo.markAllRead(userId);
    }

    public int getUnreadCount(Long userId) {
        return repo.countUnread(userId);
    }

    // 签到
    public void initSignin(Long arrangementId) {
        repo.initSignin(arrangementId);
    }

    public void signin(Long arrangementId, int type) {
        repo.signin(arrangementId, type);
    }

    public void markAbsent(Long arrangementId) {
        repo.markAbsent(arrangementId);
    }

    public List<Map<String, Object>> getSignins() {
        return repo.findSignins();
    }

    // 证书
    public void issueCertificate(Long registrationId, String certificateNo, String tradeName, String levelName, String issuer) {
        repo.insertCertificate(registrationId, certificateNo, tradeName, levelName, issuer);
    }

    public List<Map<String, Object>> getCertificatesByUser(Long userId) {
        return repo.findCertificatesByUser(userId);
    }

    public List<Map<String, Object>> getAllCertificates() {
        return repo.findAllCertificates();
    }

    public Map<String, Object> getCertificateByRegistration(Long registrationId) {
        return repo.findCertificateByRegistration(registrationId);
    }

    // 编排配置
    public Map<String, Object> getConfig(Long planId) {
        return repo.findConfigByPlan(planId);
    }

    public void saveConfig(Long planId, int defaultSeatCount, int seatGap, int shuffleUnit) {
        repo.upsertConfig(planId, defaultSeatCount, seatGap, shuffleUnit);
    }

    // 登录日志
    public List<Map<String, Object>> getLoginLogs(String username, Integer result) {
        return repo.findLoginLogs(username, result);
    }

    // 监考
    public List<Map<String, Object>> getInvigilators() {
        return repo.findInvigilators();
    }

    public void addInvigilator(String name, String phone) {
        repo.insertInvigilator(name, phone);
    }

    public void deleteInvigilator(Long id) {
        repo.deleteInvigilator(id);
    }
}
