package com.demo.module.exam.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Registration(
        Long id,
        Long userId,
        Long planId,
        Integer workYears,
        String education,
        String emergencyContact,
        String emergencyPhone,
        Integer status,
        String rejectReason,
        Long firstAuditBy,
        LocalDateTime firstAuditAt,
        Long secondAuditBy,
        LocalDateTime secondAuditAt,
        LocalDateTime submittedAt,
        LocalDateTime createdAt,
        // 关联字段
        String realName,
        String idCard,
        String phone,
        String email,
        String workUnit,
        String planName,
        String tradeName,
        String levelName,
        BigDecimal fee,
        // 编排字段
        String roomCode,
        String building,
        String classroom,
        Integer seatNo,
        String ticketNo
) {
    public String statusName() {
        return switch (status == null ? -1 : status) {
            case 0 -> "待提交";
            case 1 -> "待审核";
            case 2 -> "审核通过";
            case 3 -> "审核退回";
            case 4 -> "已缴费";
            case 5 -> "已确认";
            case 6 -> "已取消";
            default -> "未知";
        };
    }
}
