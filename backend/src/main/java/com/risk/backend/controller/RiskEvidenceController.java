package com.risk.backend.controller;

import com.risk.backend.model.Result;
import com.risk.backend.risk.model.RiskEvidence;
import com.risk.backend.risk.service.RiskEvidenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/risk/evidence")
public class RiskEvidenceController {

    private final RiskEvidenceService riskEvidenceService;

    public RiskEvidenceController(RiskEvidenceService riskEvidenceService) {
        this.riskEvidenceService = riskEvidenceService;
    }

    @PostMapping
    public Result add(@Valid @RequestBody AddEvidenceRequest request) {
        RiskEvidence ev = riskEvidenceService.addEvidence(
                request.chatId().trim(),
                request.evidenceType(),
                request.content(),
                request.sources(),
                request.metadata()
        );
        return Result.success(ev);
    }

    @GetMapping("/search")
    public Result search(
            @RequestParam @NotBlank(message = "chatId is required") String chatId,
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "5") Integer topK) {
        return Result.success(riskEvidenceService.search(chatId.trim(), query, topK));
    }

    @DeleteMapping
    public Result clear(@RequestParam @NotBlank(message = "chatId is required") String chatId) {
        riskEvidenceService.clear(chatId.trim());
        return Result.success();
    }

    public record AddEvidenceRequest(
            @NotBlank(message = "chatId is required") String chatId,
            String evidenceType,
            String content,
            List<String> sources,
            Map<String, Object> metadata
    ) {}
}

