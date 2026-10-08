package com.demo.module.exam.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 考场编排参数配置（arrangement_config），按考试计划维度保存。
 */
@Repository
public class ArrangementConfigRepository {

    private final JdbcTemplate jdbc;

    public ArrangementConfigRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> findByPlan(Long planId) {
        List<Map<String, Object>> list = jdbc.queryForList("SELECT * FROM arrangement_config WHERE plan_id = ?", planId);
        return list.isEmpty() ? null : list.get(0);
    }

    public int upsert(Long planId, int defaultSeatCount, int seatGap, int shuffleUnit) {
        return jdbc.update(
                "INSERT INTO arrangement_config (plan_id, default_seat_count, seat_gap, shuffle_unit) VALUES (?,?,?,?) " +
                        "ON DUPLICATE KEY UPDATE default_seat_count=?, seat_gap=?, shuffle_unit=?",
                planId, defaultSeatCount, seatGap, shuffleUnit, defaultSeatCount, seatGap, shuffleUnit);
    }
}
