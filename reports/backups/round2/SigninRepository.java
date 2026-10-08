package com.demo.module.room.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 考试签到（exam_signin）。
 */
@Repository
public class SigninRepository {

    private static final String SELECT_SIGNIN =
            "SELECT es.*, ra.ticket_no, ra.room_id, ra.seat_no, reg.user_id, su.real_name, " +
            "ep.plan_name, er.room_code, er.building, er.classroom " +
            "FROM exam_signin es " +
            "JOIN room_arrangement ra ON es.arrangement_id = ra.id " +
            "JOIN registration reg ON ra.registration_id = reg.id " +
            "JOIN sys_user su ON reg.user_id = su.id " +
            "JOIN exam_plan ep ON reg.plan_id = ep.id " +
            "JOIN exam_room er ON ra.room_id = er.id ";

    private final JdbcTemplate jdbc;

    public SigninRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 编排落库后按报名 ID 补建签到记录（幂等） */
    public int initByRegistration(Long registrationId) {
        return jdbc.update(
                "INSERT INTO exam_signin (arrangement_id) " +
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
        return jdbc.update("UPDATE exam_signin SET signin_time = NOW(), signin_type = ?, status = 1 WHERE arrangement_id = ?",
                signinType, arrangementId);
    }

    /**
     * 考生自助签到：仅允许更新属于本人报名记录的编排签到行。
     * @return 受影响行数；0 表示编排不存在或不属于该考生
     */
    public int signinForUser(Long arrangementId, Long userId, int signinType) {
        return jdbc.update(
                "UPDATE exam_signin es " +
                "JOIN room_arrangement ra ON es.arrangement_id = ra.id " +
                "JOIN registration reg ON ra.registration_id = reg.id " +
                "SET es.signin_time = NOW(), es.signin_type = ?, es.status = 1 " +
                "WHERE es.arrangement_id = ? AND reg.user_id = ?",
                signinType, arrangementId, userId);
    }

    public int markAbsent(Long arrangementId) {
        return jdbc.update("UPDATE exam_signin SET status = 2 WHERE arrangement_id = ?", arrangementId);
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(SELECT_SIGNIN + "ORDER BY ra.room_id, ra.seat_no");
    }

    /** 考生查询本人的考试签到记录（含考场/座位编排信息） */
    public List<Map<String, Object>> findByUserId(Long userId) {
        return jdbc.queryForList(
                SELECT_SIGNIN + "WHERE reg.user_id = ? ORDER BY ra.room_id, ra.seat_no",
                userId);
    }
}
