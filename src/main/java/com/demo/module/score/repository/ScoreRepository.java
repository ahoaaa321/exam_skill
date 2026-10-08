package com.demo.module.score.repository;

import static com.demo.common.persistence.JdbcSupport.firstOrEmpty;
import static com.demo.common.persistence.JdbcSupport.getString;
import static com.demo.common.persistence.JdbcSupport.toLocalDateTime;

import com.demo.common.constant.ScoreResult;
import com.demo.module.score.entity.Score;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ScoreRepository {

    private final JdbcTemplate jdbc;

    public ScoreRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Score> mapper = (rs, row) -> new Score(
            rs.getLong("id"),
            rs.getLong("registration_id"),
            rs.getBigDecimal("theory_score"),
            rs.getBigDecimal("practice_score"),
            rs.getBigDecimal("comprehensive_score"),
            rs.getBigDecimal("theory_weight"),
            rs.getBigDecimal("practice_weight"),
            (Integer) rs.getObject("result"),
            rs.getInt("published"),
            toLocalDateTime(rs.getTimestamp("published_at")),
            getString(rs, "plan_name"),
            getString(rs, "trade_name"),
            getString(rs, "level_name"),
            getString(rs, "real_name")
    );

    public List<Score> findByUserId(Long userId) {
        String sql = """
                SELECT s.*, p.plan_name, t.trade_name, l.level_name, u.real_name
                FROM score s
                JOIN registration r ON s.registration_id = r.id
                JOIN sys_user u ON r.user_id = u.id
                JOIN exam_plan p ON r.plan_id = p.id
                JOIN trade t ON p.trade_id = t.id
                JOIN skill_level l ON p.level_id = l.id
                WHERE r.user_id = ? AND s.published = 1
                ORDER BY s.created_at DESC
                """;
        return jdbc.query(sql, mapper, userId);
    }

    public List<Score> findAll() {
        String sql = """
                SELECT s.*, p.plan_name, t.trade_name, l.level_name, u.real_name
                FROM score s
                JOIN registration r ON s.registration_id = r.id
                JOIN sys_user u ON r.user_id = u.id
                JOIN exam_plan p ON r.plan_id = p.id
                JOIN trade t ON p.trade_id = t.id
                JOIN skill_level l ON p.level_id = l.id
                ORDER BY s.created_at DESC
                """;
        return jdbc.query(sql, mapper);
    }

    public Optional<Score> findByRegistrationId(Long registrationId) {
        String sql = """
                SELECT s.*, p.plan_name, t.trade_name, l.level_name, u.real_name
                FROM score s
                JOIN registration r ON s.registration_id = r.id
                JOIN sys_user u ON r.user_id = u.id
                JOIN exam_plan p ON r.plan_id = p.id
                JOIN trade t ON p.trade_id = t.id
                JOIN skill_level l ON p.level_id = l.id
                WHERE s.registration_id = ?
                """;
        return firstOrEmpty(jdbc.query(sql, mapper, registrationId));
    }

    public int upsert(Long registrationId, BigDecimal theory, BigDecimal practice,
                      BigDecimal theoryWeight, BigDecimal practiceWeight, BigDecimal passScore, Long operatorId) {
        BigDecimal comprehensive = theory.multiply(theoryWeight).add(practice.multiply(practiceWeight));
        int result = comprehensive.compareTo(passScore) >= 0 ? ScoreResult.PASS : ScoreResult.FAIL;
        // 修复：原 SQL 占位符与列错位（result 恒为 1、published 被写入 result）
        return jdbc.update(
                "INSERT INTO score (registration_id, theory_score, practice_score, comprehensive_score, " +
                        "theory_weight, practice_weight, result, published, created_by) " +
                        "VALUES (?,?,?,?,?,?,?,0,?) " +
                        "ON DUPLICATE KEY UPDATE theory_score=VALUES(theory_score), practice_score=VALUES(practice_score), " +
                        "comprehensive_score=VALUES(comprehensive_score), result=VALUES(result), " +
                        "published=0, published_at=NULL, created_by=VALUES(created_by)",
                registrationId, theory, practice, comprehensive, theoryWeight, practiceWeight, result, operatorId);
    }

    /** 成绩修改留痕 */
    public int insertChangeLog(Long scoreId, String oldValue, String newValue, String reason, Long operatorId) {
        return jdbc.update(
                "INSERT INTO score_change_log (score_id, old_value, new_value, reason, operator_id) VALUES (?,?,?,?,?)",
                scoreId, oldValue, newValue, reason, operatorId);
    }

    /** 某条报名对应成绩的修改留痕（含操作人姓名） */
    public List<Map<String, Object>> findChangeLogs(Long registrationId) {
        return jdbc.queryForList(
                "SELECT l.old_value, l.new_value, l.reason, l.created_at, u.real_name AS operator_name " +
                        "FROM score_change_log l " +
                        "JOIN score s ON l.score_id = s.id " +
                        "LEFT JOIN sys_user u ON l.operator_id = u.id " +
                        "WHERE s.registration_id = ? ORDER BY l.id ASC", registrationId);
    }

    /** 通过报名记录取考生用户 ID（用于发消息） */
    public Long findUserIdByRegistration(Long registrationId) {
        List<Long> list = jdbc.query("SELECT user_id FROM registration WHERE id = ?",
                (rs, n) -> rs.getLong("user_id"), registrationId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 已发布成绩合格情况统计 */
    public Map<String, Object> resultStats() {
        return jdbc.queryForMap(
                "SELECT COUNT(*) AS published, " +
                        "SUM(CASE WHEN result = 1 THEN 1 ELSE 0 END) AS passed, " +
                        "AVG(comprehensive_score) AS avgScore FROM score WHERE published = 1");
    }

    /** 发布成绩（条件更新：未发布才可发布，重复发布返回 0 行，避免重复通知/重复发证） */
    public int publish(Long registrationId) {
        return jdbc.update("UPDATE score SET published = 1, published_at = NOW() WHERE registration_id = ? AND published = 0", registrationId);
    }
}
