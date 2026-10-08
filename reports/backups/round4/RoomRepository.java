package com.demo.module.room.repository;

import com.demo.module.room.entity.ExamRoom;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RoomRepository {

    private final JdbcTemplate jdbc;

    public RoomRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<ExamRoom> mapper = (rs, row) -> new ExamRoom(
            rs.getLong("id"),
            rs.getString("room_code"),
            rs.getString("building"),
            rs.getString("classroom"),
            rs.getInt("seat_count"),
            rs.getInt("status")
    );

    public List<ExamRoom> findAll() {
        return jdbc.query("SELECT * FROM exam_room ORDER BY room_code", mapper);
    }

    public List<ExamRoom> findEnabled() {
        return jdbc.query("SELECT * FROM exam_room WHERE status = 1 ORDER BY room_code", mapper);
    }

    public Optional<ExamRoom> findById(Long id) {
        List<ExamRoom> list = jdbc.query("SELECT * FROM exam_room WHERE id = ?", mapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public int insert(ExamRoom room) {
        return jdbc.update("INSERT INTO exam_room (room_code, building, classroom, seat_count, status) VALUES (?,?,?,?,?)",
                room.roomCode(), room.building(), room.classroom(), room.seatCount(), room.status() == null ? 1 : room.status());
    }

    public int update(ExamRoom room) {
        return jdbc.update("UPDATE exam_room SET room_code=?, building=?, classroom=?, seat_count=?, status=? WHERE id=?",
                room.roomCode(), room.building(), room.classroom(), room.seatCount(), room.status(), room.id());
    }

    public int updateStatus(Long id, int status) {
        return jdbc.update("UPDATE exam_room SET status = ? WHERE id = ?", status, id);
    }

    public int delete(Long id) {
        return jdbc.update("DELETE FROM exam_room WHERE id = ?", id);
    }

    public int totalSeats() {
        return jdbc.queryForObject("SELECT COALESCE(SUM(seat_count),0) FROM exam_room WHERE status = 1", Integer.class);
    }
}
