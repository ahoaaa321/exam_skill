package com.demo.module.system.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.demo.common.Result;
import com.demo.module.system.service.AiQaService;

/** AI 智能问答（允许匿名访问，见 JwtAuthFilter 公开路径配置）。 */
@RestController
@RequestMapping("/api/system")
@CrossOrigin
public class AiQaController {

    private final AiQaService aiQaService;

    public AiQaController(AiQaService aiQaService) {
        this.aiQaService = aiQaService;
    }

    @PostMapping("/ai-qa")
    public Result<Map<String, Object>> aiQa(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入问题");
        }
        return Result.ok(aiQaService.ask(question));
    }
}
