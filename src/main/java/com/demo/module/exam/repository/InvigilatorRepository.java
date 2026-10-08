package com.demo.module.exam.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 监考人员（invigilator）。
 */
@Repository
public class InvigilatorRepository {

    private final JdbcTemplate jdbc;

    public InvigilatorRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList("SELECT * FROM invigilator ORDER BY created_at");
    }

    public int insert(String name, String phone) {
        return jdbc.update("INSERT INTO invigilator (name, phone) VALUES (?,?)", name, phone);
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM invigilator WHERE id = ?", id);
    }
}
