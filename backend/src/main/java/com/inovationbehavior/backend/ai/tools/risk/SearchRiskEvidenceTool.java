package com.inovationbehavior.backend.ai.tools.risk;

import com.inovationbehavior.backend.risk.service.RiskEvidenceService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 检索当前会话的用户补充证据片段（MVP：关键词匹配）。
 */
@Component
public class SearchRiskEvidenceTool {

    private final RiskEvidenceService riskEvidenceService;

    public SearchRiskEvidenceTool(RiskEvidenceService riskEvidenceService) {
        this.riskEvidenceService = riskEvidenceService;
    }

    @Tool(
            name = "search_risk_evidence",
            description = "Search user-provided enterprise risk evidence snippets by chat_id and query. Returns concise evidence excerpts. Use when you need concrete CVE/control/incident/policy evidence."
    )
    public String searchEvidence(
            @ToolParam(description = "The chat/session id used by the agent") String chat_id,
            @ToolParam(description = "Search query keywords/intention, e.g. \"CVE-2021\", \"access control\", \"incident\"") String query,
            @ToolParam(description = "Top K evidence items to return") Integer topK) {
        if (chat_id == null || chat_id.isBlank()) {
            return "[search_risk_evidence] missing chat_id.";
        }
        int k = topK == null ? 5 : Math.max(1, topK);
        List<com.inovationbehavior.backend.risk.model.RiskEvidence> hits = riskEvidenceService.search(chat_id, query, k);
        if (hits == null || hits.isEmpty()) {
            // fallback：返回最近证据，避免 agent 直接“无证据”
            hits = riskEvidenceService.recent(chat_id, k);
        }
        if (hits == null || hits.isEmpty()) {
            return "[search_risk_evidence] no evidence found for chat_id=" + chat_id + ".";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[RiskEvidence]\n");
        for (var ev : hits) {
            sb.append("- id=").append(ev.id())
                    .append(" | type=").append(ev.evidenceType())
                    .append(" | content=").append(ev.content() != null ? ev.content() : "")
                    .append("\n");
        }
        return sb.toString().trim();
    }
}

