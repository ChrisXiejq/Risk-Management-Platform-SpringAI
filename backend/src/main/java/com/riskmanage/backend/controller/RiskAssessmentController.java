package com.inovationbehavior.backend.controller;

import com.inovationbehavior.backend.ai.app.IBApp;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 企业安全风险评估 Agent 入口（领域化 API）。
 * <p>
 * 内部直接复用现有 LangGraph4j 图执行：IBApp -> P&E + Replan + Reflexion。
 */
@RestController
@RequestMapping("/risk")
@Validated
public class RiskAssessmentController {

    @Resource
    private IBApp ibApp;

    @PostMapping(value = "/assess", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AgentResponse assess(@Valid @RequestBody AgentRequest request) {
        String chatId = request.chatId() == null || request.chatId().isBlank() ? "default" : request.chatId().trim();
        String content = ibApp.doChatWithMultiAgentOrFull(request.message(), chatId);
        return new AgentResponse(true, content, chatId, null);
    }

    public record AgentRequest(
            @NotBlank(message = "message is required") String message,
            String chatId
    ) {}

    public record AgentResponse(
            boolean success,
            String content,
            String chatId,
            String error
    ) {}
}

