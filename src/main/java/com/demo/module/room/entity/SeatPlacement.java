package com.demo.module.room.entity;

/** 考场编排座位记录（room_arrangement 的内部类型化视图）。 */
public record SeatPlacement(
        Long id,
        Long registrationId,
        Long roomId,
        Integer seatNo,
        java.time.LocalDate examDate
) {
}
