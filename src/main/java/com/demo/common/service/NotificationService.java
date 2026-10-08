package com.demo.common.service;

import org.springframework.stereotype.Service;

import com.demo.module.system.repository.SystemRepository;
import com.demo.module.user.repository.UserRepository;

/**
 * 站内消息通知服务。在审核、缴费、编排、成绩、证书等节点向考生推送消息，
 * 同时异步发送邮件提醒（见 EmailService.sendNotification，不阻塞主流程）。
 */
@Service
public class NotificationService {

    private final SystemRepository systemRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public NotificationService(SystemRepository systemRepository, UserRepository userRepository,
                               EmailService emailService) {
        this.systemRepository = systemRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public void notify(Long userId, String title, String content) {
        if (userId == null) return;
        try {
            systemRepository.insertMessage(userId, title, content);
        } catch (Exception ignored) {
            // 通知失败不阻断主业务
        }
        // 异步发送邮件提醒（EmailService 内部投递到 mailExecutor）
        try {
            userRepository.findById(userId).ifPresent(u -> {
                if (u.email() != null && !u.email().isBlank()) {
                    emailService.sendNotification(u.email(), title, content);
                }
            });
        } catch (Exception ignored) {
            // 邮件发送失败不阻断主业务
        }
    }
}
