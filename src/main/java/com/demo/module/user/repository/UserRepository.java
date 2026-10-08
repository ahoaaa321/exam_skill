package com.demo.module.user.repository;

import static com.demo.common.persistence.JdbcSupport.firstOrEmpty;
import static com.demo.common.persistence.JdbcSupport.toLocalDateTime;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.demo.module.user.entity.User;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<User> mapper = (rs, rowNum) -> map(rs);

    private User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("real_name"),
                rs.getString("id_card"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("recovery_token"),
                (Integer) rs.getObject("gender"),
                rs.getString("work_unit"),
                (Integer) rs.getObject("age"),
                rs.getString("occupation"),
                rs.getString("income_range"),
                rs.getString("region"),
                rs.getInt("role"),
                rs.getInt("status"),
                rs.getInt("must_change_pwd"),
                rs.getInt("login_fail_count"),
                toLocalDateTime(rs.getTimestamp("lock_until")),
                toLocalDateTime(rs.getTimestamp("last_login_time")),
                rs.getString("last_login_ip"),
                toLocalDateTime(rs.getTimestamp("created_at"))
        );
    }

    public Optional<User> findById(Long id) {
        return firstOrEmpty(jdbc.query("SELECT * FROM sys_user WHERE id=?", mapper, id));
    }

    public Optional<User> findByUsername(String username) {
        return firstOrEmpty(jdbc.query("SELECT * FROM sys_user WHERE username=? OR phone=?", mapper, username, username));
    }

    public Optional<User> findByEmail(String email) {
        return firstOrEmpty(jdbc.query("SELECT * FROM sys_user WHERE email=?", mapper, email));
    }

    /** 邮箱+用户名精确匹配（系统公共邮箱多账号场景下定位具体账号） */
    public Optional<User> findByEmailAndUsername(String email, String username) {
        return firstOrEmpty(jdbc.query("SELECT * FROM sys_user WHERE email=? AND username=?", mapper, email, username));
    }

    public Optional<User> findByRecoveryToken(String token) {
        return firstOrEmpty(jdbc.query("SELECT * FROM sys_user WHERE recovery_token=?", mapper, token));
    }

    public int clearRecoveryToken(Long id) {
        return jdbc.update("UPDATE sys_user SET recovery_token=NULL WHERE id=?", id);
    }

    public List<User> findAll() {
        return jdbc.query("SELECT * FROM sys_user ORDER BY id DESC", mapper);
    }

    public int insert(User user) {
        return jdbc.update(
                "INSERT INTO sys_user (username, password, real_name, id_card, phone, email, recovery_token, gender, work_unit, age, occupation, income_range, region, role, status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                user.username(), user.password(), user.realName(), user.idCard(), user.phone(),
                user.email(), user.recoveryToken(), user.gender(), user.workUnit(),
                user.age(), user.occupation(), user.incomeRange(), user.region(),
                user.role() == null ? 0 : user.role(),
                user.status() == null ? 1 : user.status());
    }

    public int updateLoginInfo(Long id, LocalDateTime loginTime, String ip) {
        return jdbc.update("UPDATE sys_user SET login_fail_count=0, lock_until=NULL, last_login_time=?, last_login_ip=? WHERE id=?",
                loginTime, ip, id);
    }

    public int incrementFailCount(Long id, int failCount, LocalDateTime lockUntil) {
        return jdbc.update("UPDATE sys_user SET login_fail_count=?, lock_until=? WHERE id=?", failCount, lockUntil, id);
    }

    public int updatePassword(Long id, String encodedPassword) {
        return jdbc.update("UPDATE sys_user SET password=?, must_change_pwd=0 WHERE id=?", encodedPassword, id);
    }

    public int updateStatus(Long id, int status) {
        return jdbc.update("UPDATE sys_user SET status=? WHERE id=?", status, id);
    }

    public int updateProfile(Long id, String realName, String phone, String email, String workUnit) {
        return jdbc.update("UPDATE sys_user SET real_name=?, phone=?, email=?, work_unit=? WHERE id=?",
                realName, phone, email, workUnit, id);
    }

    public int updateProfileExtended(Long id, Integer age, String occupation, String incomeRange, String region) {
        return jdbc.update("UPDATE sys_user SET age=?, occupation=?, income_range=?, region=? WHERE id=?",
                age, occupation, incomeRange, region, id);
    }

    public int resetPassword(Long id, String encodedPassword) {
        return jdbc.update("UPDATE sys_user SET password=?, must_change_pwd=1 WHERE id=?", encodedPassword, id);
    }

    public int updateRole(Long id, int role) {
        return jdbc.update("UPDATE sys_user SET role=? WHERE id=?", role, id);
    }

    public int countByIdCard(String idCard) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE id_card=?", Integer.class, idCard);
    }

    public int countByUsername(String username) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE username=?", Integer.class, username);
    }
}
