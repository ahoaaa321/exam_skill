package com.demo.module.exam.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ExtendedRepository {

    private final JdbcTemplate jdbc;

    public ExtendedRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ============ 报名材料 ============
    public int insertMaterial(Long registrationId, String fileName, String filePath, long fileSize, String fileType) {
        return jdbc.update("INSERT INTO registration_material (registration_id, file_name, file_path, file_size, file_type) VALUES (?,?,?,?,?)",
                registrationId, fileName, filePath, fileSize, fileType);
    }

    public List<Map<String, Object>> findMaterialsByRegistration(Long registrationId) {
        return jdbc.queryForList("SELECT * FROM registration_material WHERE registration_id = ? ORDER BY created_at", registrationId);
    }

    public int deleteMaterial(Long id) {
        return jdbc.update("DELETE FROM registration_material WHERE id = ?", id);
    }

    /** 通过材料 ID 查询所属报名记录 ID */
    public Long findRegistrationIdByMaterial(Long materialId) {
        List<Long> list = jdbc.query("SELECT registration_id FROM registration_material WHERE id = ?",
                (rs, n) -> rs.getLong(1), materialId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 查询报名记录所属考生用户 ID */
    public Long findRegistrationUserId(Long registrationId) {
        List<Long> list = jdbc.query("SELECT user_id FROM registration WHERE id = ?",
                (rs, n) -> rs.getLong(1), registrationId);
        return list.isEmpty() ? null : list.get(0);
    }

    // ============ 审核记录 ============
    public int insertAuditRecord(Long registrationId, Long auditorId, int auditLevel, int auditResult, String reason) {
        return jdbc.update("INSERT INTO audit_record (registration_id, auditor_id, audit_level, audit_result, reason) VALUES (?,?,?,?,?)",
                registrationId, auditorId, auditLevel, auditResult, reason);
    }

    public List<Map<String, Object>> findAuditRecords(Long registrationId) {
        return jdbc.queryForList("SELECT ar.*, u.real_name AS auditor_name FROM audit_record ar LEFT JOIN sys_user u ON ar.auditor_id = u.id WHERE ar.registration_id = ? ORDER BY ar.created_at", registrationId);
    }

    // ============ 支付记录 ============
    public int insertPayment(Long registrationId, java.math.BigDecimal amount, String payMethod) {
        return jdbc.update("INSERT INTO payment_record (registration_id, amount, pay_method, pay_status, paid_at) VALUES (?,?,?,1,NOW())",
                registrationId, amount, payMethod);
    }

    public List<Map<String, Object>> findPaymentsByRegistration(Long registrationId) {
        return jdbc.queryForList("SELECT * FROM payment_record WHERE registration_id = ? ORDER BY created_at", registrationId);
    }

    public List<Map<String, Object>> findAllPayments() {
        return jdbc.queryForList("SELECT pr.*, r.real_name, p.plan_name FROM payment_record pr JOIN registration reg ON pr.registration_id = reg.id JOIN sys_user r ON reg.user_id = r.id JOIN exam_plan p ON reg.plan_id = p.id ORDER BY pr.created_at DESC");
    }

    // ============ 消息通知 ============
    public int insertMessage(Long userId, String title, String content) {
        return jdbc.update("INSERT INTO message_notification (user_id, title, content) VALUES (?,?,?)", userId, title, content);
    }

    public List<Map<String, Object>> findMessagesByUser(Long userId) {
        return jdbc.queryForList("SELECT * FROM message_notification WHERE user_id = ? ORDER BY created_at DESC", userId);
    }

    public int markMessageRead(Long id) {
        return jdbc.update("UPDATE message_notification SET is_read = 1 WHERE id = ?", id);
    }

    public int markAllRead(Long userId) {
        return jdbc.update("UPDATE message_notification SET is_read = 1 WHERE user_id = ? AND is_read = 0", userId);
    }

    public int countUnread(Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM message_notification WHERE user_id = ? AND is_read = 0", Integer.class, userId);
    }

    // ============ 考试签到 ============
    public int initSignin(Long arrangementId) {
        // INSERT IGNORE + uk_signin_arrangement 唯一索引，保证并发下幂等不重复建行
        return jdbc.update("INSERT IGNORE INTO exam_signin (arrangement_id) SELECT ? FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM exam_signin WHERE arrangement_id = ?)", arrangementId, arrangementId);
    }

    /** 编排落库后按报名ID补建签到记录（幂等） */
    public int initSigninByRegistration(Long registrationId) {
        return jdbc.update(
                "INSERT IGNORE INTO exam_signin (arrangement_id) " +
                        "SELECT ra.id FROM room_arrangement ra " +
                        "WHERE ra.registration_id = ? " +
                        "AND NOT EXISTS (SELECT 1 FROM exam_signin s WHERE s.arrangement_id = ra.id)",
                registrationId);
    }

    /** 清理指向已删除编排的孤儿签到记录（重置编排时调用） */
    public int deleteOrphanSignins() {
        return jdbc.update(
                "DELETE FROM exam_signin WHERE arrangement_id NOT IN (SELECT id FROM room_arrangement)");
    }

    public int signin(Long arrangementId, int signinType) {
        return jdbc.update("UPDATE exam_signin SET signin_time = NOW(), signin_type = ?, status = 1 WHERE arrangement_id = ?", signinType, arrangementId);
    }

    public int markAbsent(Long arrangementId) {
        return jdbc.update("UPDATE exam_signin SET status = 2 WHERE arrangement_id = ?", arrangementId);
    }

    public List<Map<String, Object>> findSignins() {
        return jdbc.queryForList("SELECT es.*, ra.ticket_no, ra.room_id, ra.seat_no, reg.user_id, su.real_name, ep.plan_name, er.room_code, er.building, er.classroom FROM exam_signin es JOIN room_arrangement ra ON es.arrangement_id = ra.id JOIN registration reg ON ra.registration_id = reg.id JOIN sys_user su ON reg.user_id = su.id JOIN exam_plan ep ON reg.plan_id = ep.id JOIN exam_room er ON ra.room_id = er.id ORDER BY ra.room_id, ra.seat_no");
    }

    // ============ 证书 ============
    public int insertCertificate(Long registrationId, String certificateNo, String tradeName, String levelName, String issuer) {
        return jdbc.update("INSERT INTO certificate (registration_id, certificate_no, trade_name, level_name, issued_at, issuer, status) VALUES (?,?,?,?,NOW(),?,1)",
                registrationId, certificateNo, tradeName, levelName, issuer);
    }

    public List<Map<String, Object>> findCertificatesByUser(Long userId) {
        return jdbc.queryForList("SELECT c.*, p.plan_name FROM certificate c JOIN registration reg ON c.registration_id = reg.id JOIN exam_plan p ON reg.plan_id = p.id WHERE reg.user_id = ? ORDER BY c.created_at DESC", userId);
    }

    public List<Map<String, Object>> findAllCertificates() {
        return jdbc.queryForList("SELECT c.*, su.real_name, su.id_card, p.plan_name FROM certificate c JOIN registration reg ON c.registration_id = reg.id JOIN sys_user su ON reg.user_id = su.id JOIN exam_plan p ON reg.plan_id = p.id ORDER BY c.created_at DESC");
    }

    public Map<String, Object> findCertificateByRegistration(Long registrationId) {
        List<Map<String, Object>> list = jdbc.queryForList("SELECT * FROM certificate WHERE registration_id = ?", registrationId);
        return list.isEmpty() ? null : list.get(0);
    }

    // ============ 编排配置 ============
    public Map<String, Object> findConfigByPlan(Long planId) {
        List<Map<String, Object>> list = jdbc.queryForList("SELECT * FROM arrangement_config WHERE plan_id = ?", planId);
        return list.isEmpty() ? null : list.get(0);
    }

    public int upsertConfig(Long planId, int defaultSeatCount, int seatGap, int shuffleUnit) {
        return jdbc.update("INSERT INTO arrangement_config (plan_id, default_seat_count, seat_gap, shuffle_unit) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE default_seat_count=?, seat_gap=?, shuffle_unit=?",
                planId, defaultSeatCount, seatGap, shuffleUnit, defaultSeatCount, seatGap, shuffleUnit);
    }

    // ============ 登录日志 ============
    public List<Map<String, Object>> findLoginLogs(String username, Integer result) {
        StringBuilder sql = new StringBuilder("SELECT * FROM sys_login_log WHERE 1=1");
        if (username != null && !username.isBlank()) sql.append(" AND username LIKE ?");
        if (result != null) sql.append(" AND login_result = ?");
        sql.append(" ORDER BY created_at DESC LIMIT 200");
        if (username != null && !username.isBlank() && result != null) {
            return jdbc.queryForList(sql.toString(), "%" + username + "%", result);
        } else if (username != null && !username.isBlank()) {
            return jdbc.queryForList(sql.toString(), "%" + username + "%");
        } else if (result != null) {
            return jdbc.queryForList(sql.toString(), result);
        }
        return jdbc.queryForList(sql.toString());
    }

    // ============ 监考人员 ============
    public List<Map<String, Object>> findInvigilators() {
        return jdbc.queryForList("SELECT * FROM invigilator ORDER BY created_at");
    }

    public int insertInvigilator(String name, String phone) {
        return jdbc.update("INSERT INTO invigilator (name, phone) VALUES (?,?)", name, phone);
    }

    public int deleteInvigilator(Long id) {
        return jdbc.update("DELETE FROM invigilator WHERE id = ?", id);
    }
}
