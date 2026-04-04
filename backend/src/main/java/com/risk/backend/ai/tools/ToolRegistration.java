package com.risk.backend.ai.tools;

import com.risk.backend.ai.memory.tool.MemoryRetrievalTool;
import com.risk.backend.ai.tools.risk.GetRiskScenarioTool;
import com.risk.backend.ai.tools.risk.SearchRiskEvidenceTool;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 集中的工具注册类（面向企业安全风险评估 Agent）。
 * <p>
 * 原项目的专利/商业化业务工具可能会引导 Agent 走偏，因此此处仅保留通用能力：
 * - 网页搜索（searchWeb）补足 RAG 证据
 * - 记忆按需检索（retrieve_history）补足上下文
 * - （可选）文件/下载/PDF 生成等非关键工具
 */
@Configuration
public class ToolRegistration {

    @Value("${search-api.api-key:}")
    private String searchApiKey;

    @Bean
    public ToolCallback[] allTools(
            @Autowired(required = false) MemoryRetrievalTool memoryRetrievalTool,
            GetRiskScenarioTool getRiskScenarioTool,
            SearchRiskEvidenceTool searchRiskEvidenceTool) {
        List<Object> toolBeans = new ArrayList<>(Arrays.asList(
                new WebSearchTool(searchApiKey),
                new WebScrapingTool(),
                new TerminateTool()
        ));
        if (memoryRetrievalTool != null) {
            toolBeans.add(memoryRetrievalTool);
        }
        toolBeans.add(getRiskScenarioTool);
        toolBeans.add(searchRiskEvidenceTool);
        return ToolCallbacks.from(toolBeans.toArray(new Object[0]));
    }
}
