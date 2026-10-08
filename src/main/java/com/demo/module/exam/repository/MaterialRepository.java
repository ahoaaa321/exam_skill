package com.demo.module.exam.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 报名材料（registration_material）与审核留痕查询（audit_record）。
 */
@Repository
public class MaterialRepository {

    private final JdbcTemplate jdbc;

    public MaterialRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(Long registrationId, String fileName, String filePath, long fileSize, String fileType) {
        return jdbc.update("INSERT INTO registration_material (registration_id, file_name, file_path, file_size, file_type) VALUES (?,?,?,?,?)",
                registrationId, fileName, filePath, fileSize, fileType);
    }

    public List<Map<String, Object>> findByRegistration(Long registrationId) {
        return jdbc.queryForList("SELECT * FROM registration_material WHERE registration_id = ? ORDER BY created_at", registrationId);
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM registration_material WHERE id = ?", id);
    }

    /** 通过材料 ID 查询所属报名记录 ID */
    public Long findRegistrationIdByMaterial(Long materialId) {
        List<Long> list = jdbc.query("SELECT registration_id FROM registration_material WHERE id = ?",
                (rs, n) -> rs.getLong(1), materialId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 查询报名记录所属考生用户 ID（材料归属校验用） */
    public Long findRegistrationUserId(Long registrationId) {
        List<Long> list = jdbc.query("SELECT user_id FROM registration WHERE id = ?",
                (rs, n) -> rs.getLong(1), registrationId);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 某条报名的审核记录时间线（含审核人姓名） */
    public List<Map<String, Object>> findAuditRecords(Long registrationId) {
        return jdbc.queryForList("SELECT ar.*, u.real_name AS auditor_name FROM audit_record ar " +
                "LEFT JOIN sys_user u ON ar.auditor_id = u.id " +
                "WHERE ar.registration_id = ? ORDER BY ar.created_at", registrationId);
    }
}
