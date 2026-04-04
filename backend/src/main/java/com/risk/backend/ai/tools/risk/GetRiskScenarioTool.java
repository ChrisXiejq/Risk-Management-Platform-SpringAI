package com.risk.backend.ai.tools.risk;

import com.risk.backend.risk.model.RiskScenario;
import com.risk.backend.risk.service.RiskScenarioService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 获取当前会话的企业风险评估场景（scope/资产/假设/约束/现有控制）。
 * Agent 在 retrieval/analysis/advice 步骤可按需调用。
 */
@Component
public class GetRiskScenarioTool {

    private final RiskScenarioService riskScenarioService;

    public GetRiskScenarioTool(RiskScenarioService riskScenarioService) {
        this.riskScenarioService = riskScenarioService;
    }

    @Tool(
            name = "get_risk_scenario",
            description = "Get the enterprise security risk assessment scenario (scope/assets/assumptions/constraints/existing controls) by chat_id."
    )
    public String getRiskScenario(
            @ToolParam(description = "The chat/session id used by the agent") String chat_id) {
        RiskScenario scenario = riskScenarioService.get(chat_id);
        if (scenario == null) {
            return "No risk scenario found for chat_id=" + chat_id + ". Please ask the user to provide scope/assets/assumptions or call searchWeb for general guidance.";
        }
        // 返回为紧凑的文本，减少 token；后续可升级为 JSON。
        StringBuilder sb = new StringBuilder();
        sb.append("[RiskScenario]\n");
        sb.append("scope: ").append(scenario.scope() != null ? scenario.scope() : "").append("\n");
        sb.append("assets_count: ").append(scenario.assets() != null ? scenario.assets().size() : 0).append("\n");
        sb.append("assumptions: ").append(scenario.assumptions() != null ? scenario.assumptions() : "").append("\n");
        sb.append("constraints: ").append(scenario.constraints() != null ? scenario.constraints() : "").append("\n");
        sb.append("existing_controls: ").append(scenario.existingControls() != null ? scenario.existingControls() : "").append("\n");
        if (scenario.assets() != null && !scenario.assets().isEmpty()) {
            sb.append("assets:\n");
            for (var a : scenario.assets()) {
                sb.append("- ").append(a.name()).append(" | type=").append(a.type())
                        .append(" | businessOwner=").append(a.businessOwner())
                        .append(" | dataClassification=").append(a.dataClassification())
                        .append(" | criticality=").append(a.criticality()).append("\n");
            }
        }
        return sb.toString().trim();
    }
}

