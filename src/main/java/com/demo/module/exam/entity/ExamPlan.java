package com.demo.module.exam.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExamPlan(
        Long id,
        String planName,
        String planCode,
        Long tradeId,
        Long levelId,
        Long categoryId,
        LocalDateTime registerStartTime,
        LocalDateTime registerEndTime,
        LocalDateTime examTime,
        LocalDateTime examEndTime,
        String examLocation,
        Integer maxCandidates,
        BigDecimal fee,
        String conditionDesc,
        Integer status,
        Integer currentCount,
        Long createdBy,
        // 关联字段
        String tradeName,
        String levelName,
        String categoryName,
        LocalDateTime createdAt
) {
    public String statusName() {
        return switch (status == null ? -1 : status) {
            case 0 -> "草稿";
            case 1 -> "已发布";
            case 2 -> "已暂停";
            case 3 -> "已关闭";
            default -> "未知";
        };
    }

    public boolean isRegisterOpen() {
        if (status == null || status != 1) return false;
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(registerStartTime) && now.isBefore(registerEndTime);
    }
}
