package com.demo.module.score.controller;

import com.demo.common.Result;
import com.demo.common.UserContext;
import com.demo.common.security.RequireRole;
import com.demo.module.score.entity.Score;
import com.demo.module.score.service.ScoreService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scores")
@CrossOrigin
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping("/my")
    public Result<List<Score>> myScores() {
        return Result.ok(scoreService.myScores(UserContext.currentUserId()));
    }

    @GetMapping
    @RequireRole({1, 2, 3})
    public Result<List<Score>> allScores() {
        return Result.ok(scoreService.allScores());
    }

    @GetMapping("/stats")
    @RequireRole({1, 2, 3})
    public Result<Map<String, Object>> stats() {
        return Result.ok(scoreService.resultStats());
    }

    @PostMapping
    @RequireRole({1, 2, 3})
    public Result<Score> save(@Valid @RequestBody ScoreRequest req) {
        return Result.ok(scoreService.saveScore(req.registrationId(), req.theory(), req.practice(),
                UserContext.currentUserId()));
    }

    @PutMapping("/{registrationId}/publish")
    @RequireRole({1, 2, 3})
    public Result<Score> publish(@PathVariable Long registrationId) {
        return Result.ok(scoreService.publishScore(registrationId, UserContext.currentUserId()));
    }

    @GetMapping("/{registrationId}/change-logs")
    @RequireRole({1, 2, 3})
    public Result<List<java.util.Map<String, Object>>> changeLogs(@PathVariable Long registrationId) {
        return Result.ok(scoreService.changeLogs(registrationId));
    }

    public record ScoreRequest(@NotNull Long registrationId, BigDecimal theory, BigDecimal practice) {}
}
