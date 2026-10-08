package com.demo.module.system.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.Result;
import com.demo.common.UserContext;
import com.demo.module.system.repository.SystemRepository;

/** 站内消息通知。 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin
public class MessageController {

    private final SystemRepository systemRepository;

    public MessageController(SystemRepository systemRepository) {
        this.systemRepository = systemRepository;
    }

    @GetMapping("/messages")
    public Result<List<Map<String, Object>>> messages() {
        return Result.ok(systemRepository.messages(UserContext.currentUserId()));
    }

    @GetMapping("/messages/unread-count")
    public Result<Integer> unreadCount() {
        return Result.ok(systemRepository.unreadCount(UserContext.currentUserId()));
    }

    @PutMapping("/messages/{id}/read")
    public Result<Void> readMessage(@PathVariable Long id) {
        systemRepository.markMessageRead(id, UserContext.currentUserId());
        return Result.ok();
    }

    @PutMapping("/messages/read-all")
    public Result<Void> readAllMessages() {
        systemRepository.markAllMessageRead(UserContext.currentUserId());
        return Result.ok();
    }
}
