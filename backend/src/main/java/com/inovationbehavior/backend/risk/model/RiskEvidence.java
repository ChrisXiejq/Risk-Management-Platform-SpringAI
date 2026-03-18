package com.inovationbehavior.backend.risk.model;

import java.util.List;
import java.util.Map;

/**
 * 用户/系统补充的证据片段（CVE/漏洞描述/控制证据/审计结论/文档摘录等）。
 */
public record RiskEvidence(
        String id,
        String chatId,
        String evidenceType,
        String content,
        List<String> sources,
        Map<String, Object> metadata
) {}

