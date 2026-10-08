package com.demo.module.exam.repository;

import static com.demo.common.persistence.JdbcSupport.firstOrEmpty;
import static com.demo.common.persistence.JdbcSupport.getString;
import static com.demo.common.persistence.JdbcSupport.toLocalDateTime;

import com.demo.module.exam.entity.ExamPlan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class PlanRepository {

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<ExamPlan> mapper = (rs, row) -> map(rs);

    private ExamPlan map(ResultSet rs) throws SQLException {
        return new ExamPlan(
                rs.getLong("id"),
                rs.getString("plan_name"),
                rs.getString("plan_code"),
                rs.getLong("trade_id"),
                rs.getLong("level_id"),
                (Long) rs.getObject("category_id"),
                toLocalDateTime(rs.getTimestamp("register_start_time")),
                toLocalDateTime(rs.getTimestamp("register_end_time")),
                toLocalDateTime(rs.getTimestamp("exam_time")),
                toLocalDateTime(rs.getTimestamp("exam_end_time")),
                rs.getString("exam_location"),
                rs.getInt("max_candidates"),
                rs.getBigDecimal("fee"),
                rs.getString("condition_desc"),
                rs.getInt("status"),
                rs.getInt("current_count"),
                rs.getLong("created_by"),
                getString(rs, "trade_name"),
                getString(rs, "level_name"),
                getString(rs, "category_name"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    public List<ExamPlan> findPublished() {
        String sql = """
                SELECT p.*, t.trade_name, l.level_name, c.category_name
                FROM exam_plan p
                LEFT JOIN trade t ON p.trade_id = t.id
                LEFT JOIN skill_level l ON p.level_id = l.id
                LEFT JOIN registration_category c ON p.category_id = c.id
                WHERE p.status = 1
                ORDER BY p.created_at DESC
                """;
        return jdbc.query(sql, mapper);
    }

    public List<ExamPlan> findAll() {
        String sql = """
                SELECT p.*, t.trade_name, l.level_name, c.category_name
                FROM exam_plan p
                LEFT JOIN trade t ON p.trade_id = t.id
                LEFT JOIN skill_level l ON p.level_id = l.id
                LEFT JOIN registration_category c ON p.category_id = c.id
                ORDER BY p.created_at DESC
                """;
        return jdbc.query(sql, mapper);
    }

    public Optional<ExamPlan> findById(Long id) {
        String sql = """
                SELECT p.*, t.trade_name, l.level_name, c.category_name
                FROM exam_plan p
                LEFT JOIN trade t ON p.trade_id = t.id
                LEFT JOIN skill_level l ON p.level_id = l.id
                LEFT JOIN registration_category c ON p.category_id = c.id
                WHERE p.id = ?
                """;
        return firstOrEmpty(jdbc.query(sql, mapper, id));
    }

    public int incrementCurrentCount(Long id) {
        return jdbc.update("UPDATE exam_plan SET current_count = current_count + 1 WHERE id = ?", id);
    }

    public int decrementCurrentCount(Long id) {
        return jdbc.update("UPDATE exam_plan SET current_count = GREATEST(current_count - 1, 0) WHERE id = ?", id);
    }

    public int updateStatus(Long id, int status) {
        return jdbc.update("UPDATE exam_plan SET status = ? WHERE id = ?", status, id);
    }

    public int insert(ExamPlan plan) {
        return jdbc.update(
                "INSERT INTO exam_plan (plan_name, plan_code, trade_id, level_id, category_id, register_start_time, register_end_time, exam_time, exam_location, max_candidates, fee, condition_desc, status, created_by) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                plan.planName(), plan.planCode(), plan.tradeId(), plan.levelId(), plan.categoryId(),
                plan.registerStartTime(), plan.registerEndTime(), plan.examTime(), plan.examLocation(),
                plan.maxCandidates(), plan.fee() == null ? BigDecimal.ZERO : plan.fee(), plan.conditionDesc(),
                plan.status() == null ? 0 : plan.status(), plan.createdBy());
    }

    public int update(ExamPlan plan) {
        return jdbc.update(
                "UPDATE exam_plan SET plan_name=?, plan_code=?, trade_id=?, level_id=?, category_id=?, register_start_time=?, register_end_time=?, exam_time=?, exam_location=?, max_candidates=?, fee=?, condition_desc=? WHERE id=?",
                plan.planName(), plan.planCode(), plan.tradeId(), plan.levelId(), plan.categoryId(),
                plan.registerStartTime(), plan.registerEndTime(), plan.examTime(), plan.examLocation(),
                plan.maxCandidates(), plan.fee() == null ? BigDecimal.ZERO : plan.fee(), plan.conditionDesc(),
                plan.id());
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM exam_plan WHERE id = ?", id);
    }
}
