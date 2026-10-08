package com.demo.module.room.controller;

import com.demo.common.Result;
import com.demo.common.security.RequireRole;
import com.demo.module.room.entity.ExamRoom;
import com.demo.module.room.repository.RoomRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin
public class RoomController {

    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping
    @RequireRole({1, 2, 3})
    public Result<List<ExamRoom>> list() {
        return Result.ok(roomRepository.findAll());
    }

    @PostMapping
    @RequireRole({2, 3})
    public Result<ExamRoom> create(@Valid @RequestBody RoomRequest req) {
        ExamRoom room = new ExamRoom(null, req.roomCode(), req.building(), req.classroom(), req.seatCount(), 1);
        roomRepository.insert(room);
        return Result.ok(room);
    }

    @PutMapping("/{id}")
    @RequireRole({2, 3})
    public Result<ExamRoom> update(@PathVariable Long id, @Valid @RequestBody RoomRequest req) {
        requireValidStatus(req.status());
        ExamRoom room = new ExamRoom(id, req.roomCode(), req.building(), req.classroom(), req.seatCount(), req.status());
        roomRepository.update(room);
        return Result.ok(room);
    }

    @PutMapping("/{id}/status")
    @RequireRole({2, 3})
    public Result<Void> toggleStatus(@PathVariable Long id, @RequestParam int status) {
        requireValidStatus(status);
        roomRepository.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @RequireRole({2, 3})
    public Result<Void> delete(@PathVariable Long id) {
        if (roomRepository.countArrangementsByRoom(id) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该考场已被考场编排引用，无法删除");
        }
        roomRepository.delete(id);
        return Result.ok();
    }

    /** 考场状态仅允许 0（禁用）/1（启用），防止任意整数落库 */
    private static void requireValidStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status 仅允许 0（禁用）或 1（启用）");
        }
    }

    public record RoomRequest(
            @NotBlank String roomCode,
            @NotBlank String building,
            @NotBlank String classroom,
            @Positive int seatCount,
            Integer status
    ) {}
}
