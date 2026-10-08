package com.demo.module.room.entity;

public record ExamRoom(
        Long id,
        String roomCode,
        String building,
        String classroom,
        Integer seatCount,
        Integer status
) {
    public String statusName() {
        return status != null && status == 1 ? "启用" : "禁用";
    }
}
