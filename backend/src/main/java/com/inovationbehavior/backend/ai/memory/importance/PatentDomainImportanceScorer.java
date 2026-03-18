package com.inovationbehavior.backend.ai.memory.importance;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 安全风险评估领域重要性评分：基于“实体/编号”与关键词加权，模拟领域 NER 效果。
 * 可配置关键词与风险编号正则，权重随轮次不在此处衰减（衰减在短期记忆层按 turn 应用）。
 */
@Component
public class PatentDomainImportanceScorer implements ImportanceScorer {

    private static final List<String> DEFAULT_KEYWORDS = List.of(
            "企业安全风险", "风险评估", "威胁", "漏洞", "CVE", "CWE",
            "资产清单", "安全控制", "治理", "合规", "ISO 27001", "NIST", "OWASP",
            "事件响应", "补丁", "访问控制", "数据保护", "风险等级", "影响", "可能性");

    /** 风险编号模式：CVE/CWE（优先）+ 合规/框架编号（兜底） */
    private static final Pattern PATENT_NUMBER = Pattern.compile(
            "(?i)\\bCVE\\s*-\\s*\\d{4}\\s*-\\s*\\d{4,}\\b"
                    + "|\\bCWE\\s*-?\\s*\\d{2,}\\b"
                    + "|\\bISO\\s*[/\\\\-]?\\s*IEC\\s*270\\d\\b"
                    + "|\\b\\d{8,15}\\b");

    private final List<String> importanceKeywords;
    private final double patentNumberWeight;
    private final double keywordWeight;
    private final double lengthFactor;
    private final double maxBaseScore;

    public PatentDomainImportanceScorer(
            @Value("${app.memory.importance.keywords:}") String keywordsConfig,
            @Value("${app.memory.importance.patent-number-weight:0.35}") double patentNumberWeight,
            @Value("${app.memory.importance.keyword-weight:0.40}") double keywordWeight,
            @Value("${app.memory.importance.length-factor:0.25}") double lengthFactor,
            @Value("${app.memory.importance.max-base-score:1.0}") double maxBaseScore) {
        List<String> parsed = (keywordsConfig != null && !keywordsConfig.isBlank())
                ? Arrays.stream(keywordsConfig.split("[,，]")).map(String::trim).filter(s -> !s.isEmpty()).toList()
                : List.of();
        this.importanceKeywords = parsed.isEmpty() ? DEFAULT_KEYWORDS : parsed;
        this.patentNumberWeight = patentNumberWeight;
        this.keywordWeight = keywordWeight;
        this.lengthFactor = lengthFactor;
        this.maxBaseScore = maxBaseScore;
    }

    @Override
    public double score(String userMessage, String assistantMessage, int turnIndex) {
        String combined = Stream.of(userMessage, assistantMessage)
                .filter(s -> s != null && !s.isBlank())
                .reduce("", (a, b) -> a + " " + b)
                .trim();
        if (combined.isBlank()) {
            return 0.0;
        }

        double score = 0.0;

        // 风险编号出现则显著加分（CVE/CWE 等）
        if (PATENT_NUMBER.matcher(combined).find()) {
            score += patentNumberWeight;
        }

        // 领域关键词命中
        String lower = combined.toLowerCase();
        long hits = importanceKeywords.stream()
                .filter(k -> !k.isBlank() && lower.contains(k.toLowerCase()))
                .count();
        if (hits > 0) {
            score += Math.min(keywordWeight, keywordWeight * 0.3 * hits);
        }

        // 长度因子：有一定信息量的对话略加分
        int len = combined.length();
        if (len >= 50) score += lengthFactor * 0.5;
        if (len >= 150) score += lengthFactor * 0.5;

        return Math.min(maxBaseScore, Math.max(0.0, score));
    }
}
