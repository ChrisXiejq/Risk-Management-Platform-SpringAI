package com.inovationbehavior.backend.ai.config;

import com.inovationbehavior.backend.ai.advisor.AgentTraceAdvisor;
import com.inovationbehavior.backend.ai.advisor.PersistingTraceAdvisor;
import com.inovationbehavior.backend.ai.audit.AgentTraceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.advisor.api.Advisor;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

class AgentTraceConfigTest {

    @Test
    void shouldCreateTracingAdvisors() {
        AgentTraceConfig config = new AgentTraceConfig();
        Advisor traceAdvisor = config.agentTraceAdvisor();
        Advisor persisting = config.persistingTraceAdvisor(mock(AgentTraceRepository.class));

        assertInstanceOf(AgentTraceAdvisor.class, traceAdvisor);
        assertInstanceOf(PersistingTraceAdvisor.class, persisting);
    }
}

