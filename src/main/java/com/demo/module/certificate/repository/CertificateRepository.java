package com.demo.module.certificate.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 职业技能等级证书（certificate）
 */
@Repository
public class CertificateRepository {

    private final JdbcTemplate jdbc;

    public CertificateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_COLS =
            "SELECT c.id, c.registration_id, c.certificate_no, c.trade_name, c.level_name, " +
                    "c.issued_at, c.issuer, c.status, u.real_name, p.plan_name, r.user_id " +
                    "FROM certificate c " +
                    "JOIN registration r ON c.registration_id = r.id " +
                    "JOIN sys_user u ON r.user_id = u.id " +
                    "JOIN exam_plan p ON r.plan_id = p.id ";

    /**
     * 成绩合格且已发布时自动生成证书（幂等）。
     * @return 受影响行数（1=新发放，0=不合格/未发布/已存在）
     */
    public int issueIfPassed(Long registrationId, String issuer) {
        return jdbc.update(
                "INSERT INTO certificate (registration_id, certificate_no, trade_name, level_name, issued_at, issuer, status) " +
                        "SELECT r.id, CONCAT('ZSZN-', YEAR(NOW()), '-', LPAD(r.id, 6, '0')), " +
                        "t.trade_name, l.level_name, CURDATE(), ?, 1 " +
                        "FROM registration r " +
                        "JOIN score s ON s.registration_id = r.id " +
                        "JOIN exam_plan p ON r.plan_id = p.id " +
                        "JOIN trade t ON p.trade_id = t.id " +
                        "JOIN skill_level l ON p.level_id = l.id " +
                        "WHERE r.id = ? AND s.result = 1 AND s.published = 1 " +
                        "AND NOT EXISTS (SELECT 1 FROM certificate c WHERE c.registration_id = r.id)",
                issuer, registrationId);
    }

    public List<Map<String, Object>> findByUserId(Long userId) {
        return jdbc.queryForList(SELECT_COLS + "WHERE r.user_id = ? ORDER BY c.id DESC", userId);
    }

    public Optional<Map<String, Object>> findByCertificateNo(String certificateNo) {
        List<Map<String, Object>> list = jdbc.queryForList(
                SELECT_COLS + "WHERE c.certificate_no = ?", certificateNo);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(SELECT_COLS + "ORDER BY c.id DESC");
    }

    public int revoke(Long id) {
        return jdbc.update("UPDATE certificate SET status = 2 WHERE id = ?", id);
    }

    public int insert(Long registrationId, String certificateNo, String tradeName, String levelName, String issuer) {
        return jdbc.update("INSERT INTO certificate (registration_id, certificate_no, trade_name, level_name, issued_at, issuer, status) VALUES (?,?,?,?,NOW(),?,1)",
                registrationId, certificateNo, tradeName, levelName, issuer);
    }
}
