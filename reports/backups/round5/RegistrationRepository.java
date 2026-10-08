package com.demo.module.exam.repository;

import static com.demo.common.persistence.JdbcSupport.firstOrEmpty;
import static com.demo.common.persistence.JdbcSupport.getString;
import static com.demo.common.persistence.JdbcSupport.toLocalDateTime;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.demo.common.constant.RegStatus;
import com.demo.module.exam.entity.Registration;

@Repository
public class RegistrationRepository {

    private final JdbcTemplate jdbc;

    public RegistrationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Registration> mapper = (rs, row) -> map(rs);

    private Registration map(ResultSet rs) throws SQLException {
        return new Registration(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("plan_id"),
                (Integer) rs.getObject("work_years"),
                rs.getString("education"),
                rs.getString("emergency_contact"),
                rs.getString("emergency_phone"),
                rs.getInt("status"),
                rs.getString("reject_reason"),
                (Long) rs.getObject("first_audit_by"),
                toLocalDateTime(rs.getTimestamp("first_audit_at")),
                (Long) rs.getObject("second_audit_by"),
                toLocalDateTime(rs.getTimestamp("second_audit_at")),
                toLocalDateTime(rs.getTimestamp("submitted_at")),
                toLocalDateTime(rs.getTimestamp("created_at")),
                getString(rs, "real_name"),
                getString(rs, "id_card"),
                getString(rs, "phone"),
                getString(rs, "email"),
                getString(rs, "work_unit"),
                getString(rs, "plan_name"),
                getString(rs, "trade_name"),
                getString(rs, "level_name"),
                rs.getBigDecimal("fee"),
                getString(rs, "room_code"),
                getString(rs, "building"),
                getString(rs, "classroom"),
                (Integer) rs.getObject("seat_no"),
                getString(rs, "ticket_no")
        );
    }

    private static final String JOIN_SQL = """
            SELECT r.*, u.real_name, u.id_card, u.phone, u.email, u.work_unit,
                   p.plan_name, t.trade_name, l.level_name, p.fee,
                   rm.room_code, rm.building, rm.classroom, ra.seat_no, ra.ticket_no
            FROM registration r
            LEFT JOIN sys_user u ON r.user_id = u.id
            LEFT JOIN exam_plan p ON r.plan_id = p.id
            LEFT JOIN trade t ON p.trade_id = t.id
            LEFT JOIN skill_level l ON p.level_id = l.id
            LEFT JOIN room_arrangement ra ON r.id = ra.registration_id
            LEFT JOIN exam_room rm ON ra.room_id = rm.id
            """;

    public List<Registration> findAll() {
        return jdbc.query(JOIN_SQL + " ORDER BY r.created_at DESC", mapper);
    }

    public List<Registration> findByUserId(Long userId) {
        return jdbc.query(JOIN_SQL + " WHERE r.user_id = ? ORDER BY r.created_at DESC", mapper, userId);
    }

    public Optional<Registration> findById(Long id) {
        return firstOrEmpty(jdbc.query(JOIN_SQL + " WHERE r.id = ?", mapper, id));
    }

    public Optional<Registration> findByUserAndPlan(Long userId, Long planId) {
        return firstOrEmpty(jdbc.query(JOIN_SQL + " WHERE r.user_id = ? AND r.plan_id = ?", mapper, userId, planId));
    }

    public int insert(Registration r) {
        return jdbc.update(
                "INSERT INTO registration (user_id, plan_id, work_years, education, emergency_contact, emergency_phone, status, submitted_at) VALUES (?,?,?,?,?,?,1,NOW())",
                r.userId(), r.planId(), r.workYears(), r.education(), r.emergencyContact(), r.emergencyPhone());
    }

    public int updateStatus(Long id, int status, String reason) {
        if (reason != null) {
            return jdbc.update("UPDATE registration SET status = ?, reject_reason = ? WHERE id = ?", status, reason, id);
        }
        return jdbc.update("UPDATE registration SET status = ?, reject_reason = NULL WHERE id = ?", status, id);
    }

    public int updateFirstAudit(Long id, Long auditorId) {
        return jdbc.update("UPDATE registration SET status = 2, first_audit_by = ?, first_audit_at = NOW(), reject_reason = NULL WHERE id = ?", auditorId, id);
    }

    /** 初审通过（条件更新：仅待审核状态可流转，防止并发/重复审核）；返回影响行数，0 表示状态已被改动 */
    public int updateFirstAuditIfPending(Long id, Long auditorId) {
        return jdbc.update("UPDATE registration SET status = 2, first_audit_by = ?, first_audit_at = NOW(), reject_reason = NULL WHERE id = ? AND status = 1", auditorId, id);
    }

    public int updateSecondAudit(Long id, Long auditorId) {
        return jdbc.update("UPDATE registration SET status = 5, second_audit_by = ?, second_audit_at = NOW() WHERE id = ?", auditorId, id);
    }

    /** 复审通过（条件更新：仅已缴费状态可流转）；返回影响行数 */
    public int updateSecondAuditIfPaid(Long id, Long auditorId) {
        return jdbc.update("UPDATE registration SET status = 5, second_audit_by = ?, second_audit_at = NOW() WHERE id = ? AND status = 4", auditorId, id);
    }

    /** 初审退回（条件更新：仅待审核状态可退回）；返回影响行数 */
    public int rejectIfPending(Long id, String reason) {
        return jdbc.update("UPDATE registration SET status = 3, reject_reason = ? WHERE id = ? AND status = 1", reason, id);
    }

    /** 复审退回（条件更新：仅已缴费状态可退回）；返回影响行数 */
    public int rejectIfPaid(Long id, String reason) {
        return jdbc.update("UPDATE registration SET status = 3, reject_reason = ? WHERE id = ? AND status = 4", reason, id);
    }

    /** 考生线上缴费成功（条件更新：仅初审通过状态可缴费，防止重复支付）；返回影响行数 */
    public int markPaidIfFirstPassed(Long id) {
        return jdbc.update("UPDATE registration SET status = 4, reject_reason = NULL WHERE id = ? AND status = 2", id);
    }

    public int reject(Long id, String reason) {
        return jdbc.update("UPDATE registration SET status = 3, reject_reason = ? WHERE id = ?", reason, id);
    }

    /** 取消报名（仅待审核/审核退回状态） */
    public int cancel(Long id) {
        return jdbc.update("UPDATE registration SET status = 6 WHERE id = ? AND status IN (1,2,3)", id);
    }

    /** 重新激活被退回或已取消的报名，重新进入待审核 */
    public int reactivate(Long id) {
        return jdbc.update("UPDATE registration SET status = 1, reject_reason = NULL, " +
                "first_audit_by = NULL, first_audit_at = NULL, second_audit_by = NULL, second_audit_at = NULL, " +
                "submitted_at = NOW() WHERE id = ?", id);
    }

    /** 写入审核留痕（初审/复审） */
    public int insertAuditRecord(Long registrationId, Long auditorId, int auditLevel, int auditResult, String reason) {
        return jdbc.update(
                "INSERT INTO audit_record (registration_id, auditor_id, audit_level, audit_result, reason) VALUES (?,?,?,?,?)",
                registrationId, auditorId, auditLevel, auditResult, reason);
    }

    /** 查询某条报名的审核记录（时间线） */
    public List<Map<String, Object>> findAuditRecords(Long registrationId) {
        return jdbc.queryForList(
                "SELECT ar.*, u.real_name AS auditor_name FROM audit_record ar " +
                        "LEFT JOIN sys_user u ON ar.auditor_id = u.id " +
                        "WHERE ar.registration_id = ? ORDER BY ar.id ASC", registrationId);
    }

    /** 重置报名编排状态（清空编排后，已确认回退到已缴费，以便重新编排） */
    public int resetArrangedStatus() {
        return jdbc.update("UPDATE registration SET status = 4 WHERE status = 5");
    }

    public List<Registration> findPaidNotArranged() {
        // 4=已缴费(可直接编排) 5=复审通过待编排；已编排的(id IN room_arrangement)排除
        return jdbc.query(JOIN_SQL + " WHERE r.status IN (4, 5) AND r.id NOT IN (SELECT registration_id FROM room_arrangement) ORDER BY r.created_at", mapper);
    }

    public int countByPlanId(Long planId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM registration WHERE plan_id = ? AND status <> 6", Integer.class, planId);
    }

    /**
     * 报名状态分布聚合（一次 GROUP BY 替代全表载入内存计数）。
     * 返回键与原 statistics() 输出保持一致：
     * total / pending / passed / paid / confirmed / rejected / canceled。
     */
    public Map<String, Object> statusCounts() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT status, COUNT(*) AS count FROM registration GROUP BY status");
        Map<Integer, Long> byStatus = new LinkedHashMap<>();
        long total = 0;
        for (Map<String, Object> row : rows) {
            int status = ((Number) row.get("status")).intValue();
            long count = ((Number) row.get("count")).longValue();
            byStatus.put(status, count);
            total += count;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("pending", byStatus.getOrDefault(RegStatus.PENDING, 0L));
        result.put("passed", byStatus.getOrDefault(RegStatus.FIRST_PASSED, 0L));
        result.put("paid", byStatus.getOrDefault(RegStatus.PAID, 0L));
        result.put("confirmed", byStatus.getOrDefault(RegStatus.CONFIRMED, 0L));
        result.put("rejected", byStatus.getOrDefault(RegStatus.REJECTED, 0L));
        result.put("canceled", byStatus.getOrDefault(RegStatus.CANCELED, 0L));
        return result;
    }
}
