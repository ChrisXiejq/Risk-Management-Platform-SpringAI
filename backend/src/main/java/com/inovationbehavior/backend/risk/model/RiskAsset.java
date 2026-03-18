package com.inovationbehavior.backend.risk.model;

import java.util.Map;

/**
 * 风险评估中的资产描述（最小可用结构，可逐步扩展）。
 */
public record RiskAsset(
        String name,
        String type,
        String businessOwner,
        String dataClassification,
        String criticality,
        Map<String, Object> attributes
) {}

