package com.risk.backend.risk.service;

import com.risk.backend.risk.model.RiskAsset;
import com.risk.backend.risk.model.RiskScenario;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MVP：用内存维护每个 chatId 的风险评估场景。
 * 与 Agent 的 tool 直接对接，避免前后端耦合。
 */
@Service
public class RiskScenarioService {

    private final Map<String, RiskScenario> scenarioByChatId = new ConcurrentHashMap<>();

    public RiskScenario upsert(RiskScenario scenario) {
        if (scenario == null || scenario.chatId() == null || scenario.chatId().isBlank()) {
            throw new IllegalArgumentException("chatId is required");
        }
        RiskScenario normalized = normalize(scenario);
        scenarioByChatId.put(normalized.chatId(), normalized);
        return normalized;
    }

    public RiskScenario get(String chatId) {
        if (chatId == null || chatId.isBlank()) return null;
        return scenarioByChatId.get(chatId);
    }

    public void clear(String chatId) {
        if (chatId == null || chatId.isBlank()) return;
        scenarioByChatId.remove(chatId);
    }

    private RiskScenario normalize(RiskScenario scenario) {
        List<RiskAsset> assets = scenario.assets() == null ? List.of() : new ArrayList<>(scenario.assets());
        List<String> assumptions = scenario.assumptions() == null ? List.of() : new ArrayList<>(scenario.assumptions());
        List<String> constraints = scenario.constraints() == null ? List.of() : new ArrayList<>(scenario.constraints());
        List<String> existingControls = scenario.existingControls() == null ? List.of() : new ArrayList<>(scenario.existingControls());
        Map<String, Object> metadata = scenario.metadata() == null ? Map.of() : scenario.metadata();
        String scope = scenario.scope() == null ? "" : scenario.scope();

        return new RiskScenario(
                scenario.chatId(),
                scope,
                assets.stream().filter(Objects::nonNull).toList(),
                assumptions.stream().filter(Objects::nonNull).toList(),
                constraints.stream().filter(Objects::nonNull).toList(),
                existingControls.stream().filter(Objects::nonNull).toList(),
                metadata
        );
    }
}

