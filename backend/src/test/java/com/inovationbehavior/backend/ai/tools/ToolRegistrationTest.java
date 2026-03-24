package com.inovationbehavior.backend.ai.tools;

import com.inovationbehavior.backend.ai.memory.tool.MemoryRetrievalTool;
import com.inovationbehavior.backend.ai.tools.risk.GetRiskScenarioTool;
import com.inovationbehavior.backend.ai.tools.risk.SearchRiskEvidenceTool;
import com.inovationbehavior.backend.risk.service.RiskEvidenceService;
import com.inovationbehavior.backend.risk.service.RiskScenarioService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ToolRegistrationTest {

    @Test
    void shouldRegisterCoreToolsAndOptionalMemoryTool() {
        ToolRegistration registration = new ToolRegistration();
        ReflectionTestUtils.setField(registration, "searchApiKey", "");

        GetRiskScenarioTool getRiskScenarioTool = new GetRiskScenarioTool(mock(RiskScenarioService.class));
        SearchRiskEvidenceTool searchRiskEvidenceTool = new SearchRiskEvidenceTool(mock(RiskEvidenceService.class));
        MemoryRetrievalTool memoryRetrievalTool = mock(MemoryRetrievalTool.class);

        ToolCallback[] withMemory = registration.allTools(memoryRetrievalTool, getRiskScenarioTool, searchRiskEvidenceTool);
        ToolCallback[] withoutMemory = registration.allTools(null, getRiskScenarioTool, searchRiskEvidenceTool);

        assertEquals(withoutMemory.length + 1, withMemory.length);
    }
}

