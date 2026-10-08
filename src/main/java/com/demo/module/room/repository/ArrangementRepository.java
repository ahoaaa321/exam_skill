package com.demo.module.room.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.demo.module.room.entity.SeatPlacement;

@Repository
public class ArrangementRepository {

    private final JdbcTemplate jdbc;

    public ArrangementRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<SeatPlacement> PLACEMENT_MAPPER = (rs, row) -> new SeatPlacement(
            rs.getLong("id"),
            rs.getLong("registration_id"),
            rs.getLong("room_id"),
            (Integer) rs.getObject("seat_no"),
            rs.getDate("exam_date") == null ? null : rs.getDate("exam_date").toLocalDate());

    public int insert(Long registrationId, Long roomId, int seatNo, String ticketNo, java.time.LocalDate examDate) {
        return jdbc.update(
                "INSERT INTO room_arrangement (registration_id, room_id, seat_no, ticket_no, exam_date) VALUES (?,?,?,?,?)",
                registrationId, roomId, seatNo, ticketNo, examDate);
    }

    /** 编排列表（管理端表格直接消费，保留下划线列名的 JSON 结构）；planId 可选过滤，尾部 LIMIT 兜底 */
    public List<Map<String, Object>> findAll(Long planId) {
        String base = "SELECT ra.*, u.real_name, u.id_card, u.phone, u.work_unit, p.plan_name, t.trade_name, l.level_name, " +
                "rm.room_code, rm.building, rm.classroom " +
                "FROM room_arrangement ra " +
                "JOIN registration r ON ra.registration_id = r.id " +
                "JOIN sys_user u ON r.user_id = u.id " +
                "JOIN exam_plan p ON r.plan_id = p.id " +
                "JOIN trade t ON p.trade_id = t.id " +
                "JOIN skill_level l ON p.level_id = l.id " +
                "JOIN exam_room rm ON ra.room_id = rm.id ";
        if (planId == null) {
            return jdbc.queryForList(base + "ORDER BY rm.room_code, ra.seat_no LIMIT 2000");
        }
        return jdbc.queryForList(base + "WHERE r.plan_id = ? ORDER BY rm.room_code, ra.seat_no LIMIT 2000", planId);
    }

    /** 指定考试日期的座位占位（编排算法内部使用：座位唯一性按考试日期时段化） */
    public List<SeatPlacement> findPlacementsByDate(java.time.LocalDate examDate) {
        return jdbc.query(
                "SELECT id, registration_id, room_id, seat_no, exam_date FROM room_arrangement WHERE exam_date = ?",
                PLACEMENT_MAPPER, examDate);
    }

    public int generateAdmissionTicket(Long registrationId, String ticketNo) {
        return jdbc.update(
                "INSERT INTO admission_ticket (registration_id, ticket_no) VALUES (?,?) " +
                        "ON DUPLICATE KEY UPDATE ticket_no = VALUES(ticket_no)",
                registrationId, ticketNo);
    }

    public int updateRegistrationStatusToConfirmed(Long registrationId) {
        return jdbc.update("UPDATE registration SET status = 5 WHERE id = ?", registrationId);
    }

    public List<Map<String, Object>> statisticsByTrade() {
        return jdbc.queryForList(
                "SELECT t.trade_name, COUNT(*) AS count FROM registration r " +
                        "JOIN exam_plan p ON r.plan_id = p.id " +
                        "JOIN trade t ON p.trade_id = t.id " +
                        "WHERE r.status <> 6 GROUP BY t.trade_name");
    }

    public List<Map<String, Object>> statisticsByLevel() {
        return jdbc.queryForList(
                "SELECT l.level_name, COUNT(*) AS count FROM registration r " +
                        "JOIN exam_plan p ON r.plan_id = p.id " +
                        "JOIN skill_level l ON p.level_id = l.id " +
                        "WHERE r.status <> 6 GROUP BY l.level_name");
    }

    public int countAll() {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM room_arrangement", Integer.class);
        return n == null ? 0 : n;
    }

    public int deleteAllTickets() {
        return jdbc.update("DELETE FROM admission_ticket");
    }

    public int deleteAllArrangements() {
        return jdbc.update("DELETE FROM room_arrangement");
    }

    public Optional<SeatPlacement> findArrangementById(Long id) {
        List<SeatPlacement> list = jdbc.query(
                "SELECT id, registration_id, room_id, seat_no, exam_date FROM room_arrangement WHERE id = ?",
                PLACEMENT_MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** 目标座位同考试日期下是否已被占用（座位唯一键按时段化后，冲突仅在相同日期内判定） */
    public Optional<SeatPlacement> findByRoomSeat(Long roomId, int seatNo, java.time.LocalDate examDate) {
        List<SeatPlacement> list = jdbc.query(
                "SELECT id, registration_id, room_id, seat_no, exam_date FROM room_arrangement WHERE room_id = ? AND seat_no = ? AND exam_date = ?",
                PLACEMENT_MAPPER, roomId, seatNo, examDate);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public int updatePlacement(Long arrangementId, Long roomId, int seatNo, String ticketNo) {
        return jdbc.update(
                "UPDATE room_arrangement SET room_id=?, seat_no=?, ticket_no=?, adjusted=1 WHERE id=?",
                roomId, seatNo, ticketNo, arrangementId);
    }

    public int syncTicketNo(Long registrationId, String ticketNo) {
        return jdbc.update("UPDATE admission_ticket SET ticket_no = ? WHERE registration_id = ?", ticketNo, registrationId);
    }

    public List<Map<String, Object>> monthlyTrend() {
        return jdbc.queryForList(
                "SELECT DATE_FORMAT(ra.created_at, '%Y-%m') AS month, COUNT(*) AS count " +
                        "FROM room_arrangement ra GROUP BY month ORDER BY month");
    }

    public List<Map<String, Object>> countGroupByStatus() {
        return jdbc.queryForList(
                "SELECT r.status, COUNT(*) AS count FROM room_arrangement ra " +
                        "JOIN registration r ON ra.registration_id = r.id GROUP BY r.status");
    }
}
