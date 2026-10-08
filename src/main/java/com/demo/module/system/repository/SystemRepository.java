package com.demo.module.system.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class SystemRepository {

    private final JdbcTemplate jdbc;

    public SystemRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> announcements() {
        return jdbc.queryForList("SELECT * FROM announcement WHERE status = 1 ORDER BY is_top DESC, created_at DESC");
    }

    public List<Map<String, Object>> allAnnouncements() {
        return jdbc.queryForList("SELECT * FROM announcement ORDER BY created_at DESC");
    }

    public int insertAnnouncement(String title, String content, int isTop, Long createdBy) {
        return jdbc.update("INSERT INTO announcement (title, content, is_top, status, created_by) VALUES (?,?,?,1,?)",
                title, content, isTop, createdBy);
    }

    public int updateAnnouncementStatus(Long id, int status) {
        return jdbc.update("UPDATE announcement SET status = ? WHERE id = ?", status, id);
    }

    public List<Map<String, Object>> trades() {
        return jdbc.queryForList("SELECT * FROM trade WHERE status = 1 ORDER BY sort_order");
    }

    public List<Map<String, Object>> allTrades() {
        return jdbc.queryForList("SELECT * FROM trade ORDER BY sort_order");
    }

    public int insertTrade(String name, String code, String category, String desc, int sortOrder) {
        return jdbc.update("INSERT INTO trade (trade_name, trade_code, trade_category, description, sort_order, status) VALUES (?,?,?,?,?,1)",
                name, code, category, desc, sortOrder);
    }

    public int updateTrade(Long id, String name, String code, String category, String desc, int sortOrder) {
        return jdbc.update("UPDATE trade SET trade_name=?, trade_code=?, trade_category=?, description=?, sort_order=? WHERE id=?",
                name, code, category, desc, sortOrder, id);
    }

    public int deleteTrade(Long id) {
        return jdbc.update("DELETE FROM trade WHERE id=?", id);
    }

    public int updateTradeStatus(Long id, int status) {
        return jdbc.update("UPDATE trade SET status=? WHERE id=?", status, id);
    }

    public List<Map<String, Object>> skillLevels() {
        return jdbc.queryForList("SELECT l.*, t.trade_name FROM skill_level l JOIN trade t ON l.trade_id = t.id WHERE l.status = 1 ORDER BY t.sort_order, l.sort_order");
    }

    public List<Map<String, Object>> allSkillLevels() {
        return jdbc.queryForList("SELECT l.*, t.trade_name FROM skill_level l JOIN trade t ON l.trade_id = t.id ORDER BY t.sort_order, l.sort_order");
    }

    public int insertSkillLevel(Long tradeId, String name, String code, int rank, int sortOrder) {
        return jdbc.update("INSERT INTO skill_level (trade_id, level_name, level_code, level_rank, sort_order, status) VALUES (?,?,?,?,?,1)",
                tradeId, name, code, rank, sortOrder);
    }

    public int updateSkillLevel(Long id, Long tradeId, String name, String code, int rank, int sortOrder) {
        return jdbc.update("UPDATE skill_level SET trade_id=?, level_name=?, level_code=?, level_rank=?, sort_order=? WHERE id=?",
                tradeId, name, code, rank, sortOrder, id);
    }

    public int deleteSkillLevel(Long id) {
        return jdbc.update("DELETE FROM skill_level WHERE id=?", id);
    }

    public int updateSkillLevelStatus(Long id, int status) {
        return jdbc.update("UPDATE skill_level SET status=? WHERE id=?", status, id);
    }

    public List<Map<String, Object>> messages(Long userId) {
        return jdbc.queryForList("SELECT * FROM message_notification WHERE user_id = ? ORDER BY created_at DESC", userId);
    }

    public int unreadCount(Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM message_notification WHERE user_id = ? AND is_read = 0", Integer.class, userId);
    }

    public int markMessageRead(Long id, Long userId) {
        return jdbc.update("UPDATE message_notification SET is_read = 1 WHERE id = ? AND user_id = ?", id, userId);
    }

    public int markAllMessageRead(Long userId) {
        return jdbc.update("UPDATE message_notification SET is_read = 1 WHERE user_id = ? AND is_read = 0", userId);
    }

    public List<Map<String, Object>> loginLogs(int limit) {
        return jdbc.queryForList("SELECT * FROM sys_login_log ORDER BY created_at DESC LIMIT ?", limit);
    }

    /** 登录日志按用户名模糊 / 登录结果过滤（最多 200 条，参数化拼接防注入） */
    public List<Map<String, Object>> loginLogs(String username, Integer result) {
        StringBuilder sql = new StringBuilder("SELECT * FROM sys_login_log WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();
        if (username != null && !username.isBlank()) {
            sql.append(" AND username LIKE ?");
            params.add("%" + username + "%");
        }
        if (result != null) {
            sql.append(" AND login_result = ?");
            params.add(result);
        }
        sql.append(" ORDER BY created_at DESC LIMIT 200");
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public boolean ping() {
        Boolean ok = jdbc.queryForObject("SELECT 1", Boolean.class);
        return Boolean.TRUE.equals(ok);
    }

    public int insertMessage(Long userId, String title, String content) {
        return jdbc.update("INSERT INTO message_notification (user_id, title, content) VALUES (?,?,?)", userId, title, content);
    }

    public List<Map<String, Object>> operationLogs(String operator, String type, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM operation_log WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();
        if (operator != null && !operator.isBlank()) {
            sql.append(" AND operator_name LIKE ?");
            params.add("%" + operator + "%");
        }
        if (type != null && !type.isBlank()) {
            sql.append(" AND operation_type = ?");
            params.add(type);
        }
        sql.append(" ORDER BY created_at DESC LIMIT ?");
        params.add(limit);
        return jdbc.queryForList(sql.toString(), params.toArray());
    }

    public int insertLog(Long operatorId, String operatorName, String type, String target, String detail, String ip) {
        return jdbc.update("INSERT INTO operation_log (operator_id, operator_name, operation_type, target, detail, client_ip) VALUES (?,?,?,?,?,?)",
                operatorId, operatorName, type, target, detail, ip);
    }

    public int insertLogWithSnapshot(Long operatorId, String operatorName, String type, String target, String detail,
                                     String beforeValue, String afterValue, String ip) {
        return jdbc.update("INSERT INTO operation_log (operator_id, operator_name, operation_type, target, detail, before_value, after_value, client_ip) VALUES (?,?,?,?,?,?,?,?)",
                operatorId, operatorName, type, target, detail, beforeValue, afterValue, ip);
    }

    public List<Map<String, Object>> configs() {
        return jdbc.queryForList("SELECT * FROM system_config");
    }

    public String getConfig(String key, String defaultValue) {
        List<String> list = jdbc.queryForList(
                "SELECT config_value FROM system_config WHERE config_key = ?", String.class, key);
        if (list.isEmpty() || list.get(0) == null || list.get(0).isBlank()) {
            return defaultValue;
        }
        return list.get(0);
    }

    public int updateConfig(String key, String value) {
        return jdbc.update("UPDATE system_config SET config_value = ? WHERE config_key = ?", value, key);
    }

    // ============ AI 问答日志 ============
    public int insertAiQaLog(Long userId, String question, String answer, String category) {
        return jdbc.update("INSERT INTO ai_qa_log (user_id, question, answer, category) VALUES (?,?,?,?)",
                userId, question, answer, category);
    }

    // ============ 用户注册按年/月统计 ============
    public List<Map<String, Object>> userRegistrationMonthly(int year) {
        return jdbc.queryForList(
                "SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, COUNT(*) AS count FROM sys_user " +
                "WHERE YEAR(created_at) = ? GROUP BY DATE_FORMAT(created_at, '%Y-%m') ORDER BY month",
                year);
    }

    // ============ 报考分类汇总 ============
    public List<Map<String, Object>> registrationCategorySummary() {
        return jdbc.queryForList(
                "SELECT t.trade_name AS trade, l.level_name AS level, " +
                "COUNT(r.id) AS total, " +
                "SUM(CASE WHEN r.status IN (2,4,5) THEN 1 ELSE 0 END) AS passed, " +
                "SUM(CASE WHEN r.status = 3 THEN 1 ELSE 0 END) AS rejected, " +
                "SUM(CASE WHEN r.status = 6 THEN 1 ELSE 0 END) AS canceled, " +
                "COALESCE(SUM(p.amount), 0) AS total_fee " +
                "FROM registration r " +
                "JOIN exam_plan e ON r.plan_id = e.id " +
                "JOIN trade t ON e.trade_id = t.id " +
                "JOIN skill_level l ON e.level_id = l.id " +
                "LEFT JOIN payment_record p ON p.registration_id = r.id AND p.pay_status = 1 " +
                "GROUP BY t.trade_name, l.level_name ORDER BY total DESC");
    }

    // ============ 公告删除 ============
    public int deleteAnnouncement(Long id) {
        return jdbc.update("DELETE FROM announcement WHERE id = ?", id);
    }

    // ============ 售后服务工单 ============
    public List<Map<String, Object>> afterSalesTickets(Long userId) {
        if (userId != null) {
            return jdbc.queryForList("SELECT t.*, u.real_name AS user_name FROM after_sales_ticket t " +
                    "LEFT JOIN sys_user u ON t.user_id = u.id WHERE t.user_id = ? ORDER BY t.created_at DESC", userId);
        }
        return jdbc.queryForList("SELECT t.*, u.real_name AS user_name FROM after_sales_ticket t " +
                "LEFT JOIN sys_user u ON t.user_id = u.id ORDER BY t.created_at DESC LIMIT 2000");
    }

    public int insertAfterSalesTicket(Long userId, Long registrationId, String title, String content, String type) {
        return jdbc.update(
                "INSERT INTO after_sales_ticket (user_id, registration_id, title, content, type) VALUES (?,?,?,?,?)",
                userId, registrationId, title, content, type);
    }

    public int replyAfterSalesTicket(Long id, String reply, Long repliedBy) {
        return jdbc.update("UPDATE after_sales_ticket SET reply=?, status=2, replied_by=?, replied_at=NOW() WHERE id=?",
                reply, repliedBy, id);
    }

    public int updateAfterSalesStatus(Long id, int status) {
        return jdbc.update("UPDATE after_sales_ticket SET status=? WHERE id=?", status, id);
    }

    /** 查询报名记录归属用户（工单提交时校验归属），不存在返回 null */
    public Long findRegistrationOwnerId(Long registrationId) {
        List<Long> list = jdbc.queryForList("SELECT user_id FROM registration WHERE id = ?", Long.class, registrationId);
        return list.isEmpty() ? null : list.get(0);
    }

    // ============ 基础数据删除引用检查（schema 无外键，删除前置校验） ============
    public int countExamPlansByTrade(Long tradeId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM exam_plan WHERE trade_id = ?", Integer.class, tradeId);
    }

    public int countSkillLevelsByTrade(Long tradeId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM skill_level WHERE trade_id = ?", Integer.class, tradeId);
    }

    public int countExamPlansByLevel(Long levelId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM exam_plan WHERE level_id = ?", Integer.class, levelId);
    }
}
