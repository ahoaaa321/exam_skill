package com.demo.module.exam.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 缴费流水（payment_record）
 */
@Repository
public class PaymentRepository {

    private final JdbcTemplate jdbc;

    public PaymentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(Long registrationId, BigDecimal amount, String payMethod, String tradeNo, int payStatus) {
        return jdbc.update(
                "INSERT INTO payment_record (registration_id, amount, pay_method, trade_no, pay_status, paid_at) " +
                        "VALUES (?,?,?,?,?,NOW())",
                registrationId, amount, payMethod, tradeNo, payStatus);
    }

    public Optional<Map<String, Object>> findByRegistrationId(Long registrationId) {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT * FROM payment_record WHERE registration_id = ? ORDER BY id DESC LIMIT 1", registrationId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList(
                "SELECT pr.*, u.real_name, u.phone, p.plan_name, r.user_id " +
                        "FROM payment_record pr " +
                        "JOIN registration r ON pr.registration_id = r.id " +
                        "JOIN sys_user u ON r.user_id = u.id " +
                        "JOIN exam_plan p ON r.plan_id = p.id " +
                        "ORDER BY pr.id DESC");
    }

    /** 已支付费用合计（实收） */
    public BigDecimal sumPaidAmount() {
        BigDecimal v = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM payment_record WHERE pay_status = 1", BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 该报名关联支付记录标记退款 */
    public int markRefund(Long registrationId) {
        return jdbc.update("UPDATE payment_record SET pay_status = 2 WHERE registration_id = ? AND pay_status = 1",
                registrationId);
    }
}
