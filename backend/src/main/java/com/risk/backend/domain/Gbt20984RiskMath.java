package com.risk.backend.domain;

/**
 * 定性风险等级（与 GB/T 20984 风险分析思路一致：在 Likelihood × Consequence 网格上映射等级）。
 */
public final class Gbt20984RiskMath {

    private Gbt20984RiskMath() {
    }

    public static int clamp15(int v) {
        return Math.max(1, Math.min(5, v));
    }

    public static String riskLevel(int likelihood, int impact) {
        int l = clamp15(likelihood);
        int i = clamp15(impact);
        int score = l * i;
        if (score >= 20) {
            return "HIGH";
        }
        if (score >= 12) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
