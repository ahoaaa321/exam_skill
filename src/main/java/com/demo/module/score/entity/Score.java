package com.demo.module.score.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Score(
        Long id,
        Long registrationId,
        BigDecimal theoryScore,
        BigDecimal practiceScore,
        BigDecimal comprehensiveScore,
        BigDecimal theoryWeight,
        BigDecimal practiceWeight,
        Integer result,
        Integer published,
        LocalDateTime publishedAt,
        // 关联
        String planName,
        String tradeName,
        String levelName,
        String realName
) {
    public String resultName() {
        return result != null && result == 1 ? "合格" : "不合格";
    }
}
