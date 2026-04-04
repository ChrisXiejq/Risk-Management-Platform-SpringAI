package com.inovationbehavior.backend.risk.model;

import java.util.List;
import java.util.Map;

/**
 * 企业风险评估场景（范围、资产、假设、约束等）。
 * 目前以内存存储为 MVP；后续可迁移为 MySQL/向量库/多租户存储。
 */
public record RiskScenario(
        String chatId,
        String scope,
        List<RiskAsset> assets,
        List<String> assumptions,
        List<String> constraints,
        List<String> existingControls,
        Map<String, Object> metadata
) {}

