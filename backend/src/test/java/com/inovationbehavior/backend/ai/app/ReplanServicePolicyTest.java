package com.inovationbehavior.backend.ai.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplanServicePolicyTest {

    @Test
    void shouldDetectRecoverableRetrievalFailure() {
        assertTrue(ReplanService.isRetrievalFailureRecoverableWithWeb("Failed to retrieve documents from pgvector"));
        assertTrue(ReplanService.isRetrievalFailureRecoverableWithWeb("Unable to connect upstream"));
        assertFalse(ReplanService.isRetrievalFailureRecoverableWithWeb("analysis success with evidence"));
    }

    @Test
    void shouldRetryRetrievalWithWebForInsufficientRetrievalStep() {
        String retrievalInsufficient = "[Task:retrieval]\nI don't know, no documents";
        assertTrue(ReplanService.shouldRetryRetrievalWithWeb(retrievalInsufficient));
        assertFalse(ReplanService.shouldRetryRetrievalWithWeb("[Task:analysis]\nI don't know"));
    }

    @Test
    void shouldJudgeInsufficientResultsByHeuristics() {
        assertTrue(ReplanService.isResultInsufficient("no evidence"));
        assertTrue(ReplanService.isResultInsufficient("暂无"));
        assertFalse(ReplanService.isResultInsufficient("已完成风险识别，含三条证据与控制项建议。"));
    }
}

