package com.demo.common.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * QQ邮箱验证码服务
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final JdbcTemplate jdbcTemplate;
    private final SecureRandom random = new SecureRandom();

    /** 邮件异步投递线程池（守护线程，不阻塞 JVM 退出） */
    private final ExecutorService mailExecutor = Executors.newFixedThreadPool(2, new java.util.concurrent.ThreadFactory() {
        private final AtomicInteger seq = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "verify-code-mailer-" + seq.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.email.verify-code-ttl-minutes:5}")
    private int ttlMinutes;

    /** 同一邮箱两次发送的最小间隔（秒） */
    private static final int RESEND_INTERVAL_SECONDS = 60;
    /** 同一邮箱 24 小时最大发送条数 */
    private static final int DAILY_LIMIT = 10;

    public EmailService(JavaMailSender mailSender, JdbcTemplate jdbcTemplate) {
        this.mailSender = mailSender;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 发送验证码到指定邮箱
     */
    public Map<String, Object> sendVerifyCode(String toEmail, String purpose) {
        if (toEmail == null || !toEmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        assertSendAllowed(toEmail);
        String code = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expireAt = LocalDateTime.now().plusMinutes(ttlMinutes);

        String subject = "【职业技能等级认定】邮箱验证码";
        String content = String.format("""
                尊敬的用户：

                您正在进行【%s】操作，您的邮箱验证码为：

                %s

                验证码有效期为 %d 分钟，请尽快使用。如非本人操作，请忽略本邮件。

                职业技能等级认定考试报名与考场编排系统
                """, purpose, code, ttlMinutes);

        // 验证码先入库（频控与校验均依赖该记录），再异步投递邮件，接口立即返回
        jdbcTemplate.update(
                "INSERT INTO sys_email_record (to_email, subject, content, send_status, verify_code, expire_at, created_at) VALUES (?,?,?,0,?,?,NOW())",
                toEmail, subject, content, code, expireAt);

        mailExecutor.submit(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(content);
                mailSender.send(message);

                jdbcTemplate.update("UPDATE sys_email_record SET send_status=1, send_time=NOW() WHERE to_email=? AND verify_code=? AND send_status=0 ORDER BY id DESC LIMIT 1",
                        toEmail, code);
            } catch (Exception e) {
                jdbcTemplate.update("UPDATE sys_email_record SET send_status=2, error_msg=? WHERE to_email=? AND verify_code=? ORDER BY id DESC LIMIT 1",
                        e.getMessage(), toEmail, code);
            }
        });
        return Map.of("sent", true, "ttl", ttlMinutes);
    }

    /**
     * 发送频控：60 秒间隔 + 24 小时上限
     */
    private void assertSendAllowed(String email) {
        LocalDateTime now = LocalDateTime.now();
        List<java.util.Map<String, Object>> recent = jdbcTemplate.queryForList(
                "SELECT created_at FROM sys_email_record WHERE to_email = ? ORDER BY id DESC LIMIT 1", email);
        if (!recent.isEmpty()) {
            Object ts = recent.get(0).get("created_at");
            if (ts instanceof java.sql.Timestamp t) {
                long elapsed = java.time.Duration.between(t.toLocalDateTime(), now).getSeconds();
                if (elapsed < RESEND_INTERVAL_SECONDS) {
                    throw new IllegalStateException("发送过于频繁，请" + (RESEND_INTERVAL_SECONDS - elapsed) + "秒后再试");
                }
            }
        }
        Integer daily = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_email_record WHERE to_email = ? AND created_at > ?",
                Integer.class, email, now.minusHours(24));
        if (daily != null && daily >= DAILY_LIMIT) {
            throw new IllegalStateException("该邮箱今日验证码发送次数已达上限，请24小时后再试");
        }
    }

    /**
     * 校验验证码
     */
    public boolean verifyCode(String email, String code) {
        if (email == null || code == null) return false;
        // 原子核销：仅当记录未使用且未过期时置 used=1，影响行数=1 才算成功，防止并发双花
        int updated = jdbcTemplate.update(
                "UPDATE sys_email_record SET used=1 WHERE to_email=? AND verify_code=? AND used=0 AND expire_at>NOW() ORDER BY id DESC LIMIT 1",
                email, code);
        return updated == 1;
    }

    /**
     * 发送站内消息邮件通知（异步投递，不阻塞调用方请求线程/事务）
     */
    public void sendNotification(String toEmail, String title, String content) {
        if (toEmail == null || toEmail.isBlank()) return;
        String subject = "【职业技能等级认定】" + title;
        String body = String.format("""
                尊敬的用户：

                您有一条新的站内消息：

                【标题】%s

                【内容】
                %s

                请登录报名平台查看详情。

                职业技能等级认定考试报名与考场编排系统
                """, title, content == null ? "" : content);

        // 与验证码发送一致：入库与 SMTP 投递均改投 mailExecutor，接口立即返回
        mailExecutor.submit(() -> {
            try {
                jdbcTemplate.update(
                        "INSERT INTO sys_email_record (to_email, subject, content, send_status, created_at) VALUES (?,?,?,0,NOW())",
                        toEmail, subject, body);
            } catch (Exception e) {
                log.warn("站内通知邮件记录入库失败: {}", e.getMessage());
            }
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                jdbcTemplate.update("UPDATE sys_email_record SET send_status=1, send_time=NOW() WHERE to_email=? AND subject=? ORDER BY id DESC LIMIT 1",
                        toEmail, subject);
            } catch (Exception e) {
                log.warn("站内通知邮件投递失败: {}", e.getMessage());
                try {
                    jdbcTemplate.update("UPDATE sys_email_record SET send_status=2, error_msg=? WHERE to_email=? AND subject=? ORDER BY id DESC LIMIT 1",
                            e.getMessage(), toEmail, subject);
                } catch (Exception ignored) {}
            }
        });
    }
}
