package com.risk.backend.controller;

import com.risk.backend.model.Result;
import com.risk.backend.risk.model.RiskAsset;
import com.risk.backend.risk.model.RiskScenario;
import com.risk.backend.risk.service.RiskScenarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/risk/scenario")
public class RiskScenarioController {

    private final RiskScenarioService riskScenarioService;

    public RiskScenarioController(RiskScenarioService riskScenarioService) {
        this.riskScenarioService = riskScenarioService;
    }

    @PostMapping
    public Result upsert(@Valid @RequestBody UpsertRiskScenarioRequest request) {
        RiskScenario scenario = new RiskScenario(
                request.chatId().trim(),
                request.scope() == null ? "" : request.scope(),
                request.assets() == null ? List.of() : request.assets(),
                request.assumptions() == null ? List.of() : request.assumptions(),
                request.constraints() == null ? List.of() : request.constraints(),
                request.existingControls() == null ? List.of() : request.existingControls(),
                request.metadata() == null ? Map.of() : request.metadata()
        );
        return Result.success(riskScenarioService.upsert(scenario));
    }

    @GetMapping
    public Result get(@NotBlank(message = "chatId is required") String chatId) {
        return Result.success(riskScenarioService.get(chatId.trim()));
    }

    @DeleteMapping
    public Result clear(@NotBlank(message = "chatId is required") String chatId) {
        riskScenarioService.clear(chatId.trim());
        return Result.success();
    }

    public record UpsertRiskScenarioRequest(
            @NotBlank(message = "chatId is required") String chatId,
            String scope,
            List<RiskAsset> assets,
            List<String> assumptions,
            List<String> constraints,
            List<String> existingControls,
            Map<String, Object> metadata
    ) {}
}

