package com.inovationbehavior.backend.ai.graph.nodes;

import com.inovationbehavior.backend.ai.app.IBApp;
import com.inovationbehavior.backend.ai.app.ReplanService;
import com.inovationbehavior.backend.ai.graph.PatentGraphState;
import com.inovationbehavior.backend.ai.reflect.ReflectionService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlannerExecutorReplanNodeTest {

    @Test
    void plannerNode_shouldInitializePlanAndIndexes() {
        IBApp ibApp = mock(IBApp.class);
        when(ibApp.createPlan(eq("评估供应链风险"), anyList(), eq("c1")))
                .thenReturn(List.of("retrieval", "analysis", "advice", "synthesize"));
        PlannerNode node = new PlannerNode(ibApp);

        PatentGraphState state = new PatentGraphState(Map.of(
                PatentGraphState.USER_MESSAGE, "评估供应链风险",
                PatentGraphState.CHAT_ID, "c1",
                PatentGraphState.STEP_RESULTS, List.of()
        ));

        Map<String, Object> out = node.apply(state);

        assertEquals(List.of("retrieval", "analysis", "advice", "synthesize"), out.get(PatentGraphState.PLAN));
        assertEquals(0, out.get(PatentGraphState.CURRENT_STEP_INDEX));
        assertEquals(0, out.get(PatentGraphState.NEED_REPLAN));
    }

    @Test
    void executorNode_shouldAppendTaskPrefixAndSetNeedReplanWhenInsufficient() {
        IBApp ibApp = mock(IBApp.class);
        when(ibApp.doReActForTask("retrieval", "查最近风险通报", "c2", List.of("old")))
                .thenReturn("I don't know");
        ExecutorNode node = new ExecutorNode(ibApp);

        PatentGraphState state = new PatentGraphState(Map.of(
                PatentGraphState.PLAN, List.of("retrieval", "analysis"),
                PatentGraphState.CURRENT_STEP_INDEX, 0,
                PatentGraphState.USER_MESSAGE, "查最近风险通报",
                PatentGraphState.CHAT_ID, "c2",
                PatentGraphState.STEP_RESULTS, List.of("old"),
                PatentGraphState.STEP_COUNT, 1
        ));

        Map<String, Object> out = node.apply(state);

        assertTrue(((String) out.get(PatentGraphState.STEP_RESULTS)).startsWith("[Task:retrieval]\n"));
        assertEquals(2, out.get(PatentGraphState.STEP_COUNT));
        assertEquals(1, out.get(PatentGraphState.CURRENT_STEP_INDEX));
        assertEquals(1, out.get(PatentGraphState.NEED_REPLAN));
    }

    @Test
    void replanNode_shouldOnlyReplanRemainingWhenEnvironmentChanged() {
        ReplanService replanService = mock(ReplanService.class);
        when(replanService.replanRemaining(eq("用户问题"), anyList(), eq(List.of("analysis", "advice"))))
                .thenReturn(List.of("analysis", "synthesize"));
        ReplanNode node = new ReplanNode(replanService);

        PatentGraphState state = new PatentGraphState(Map.of(
                PatentGraphState.USER_MESSAGE, "用户问题",
                PatentGraphState.STEP_RESULTS, List.of("step1"),
                PatentGraphState.PLAN, List.of("retrieval", "analysis", "advice"),
                PatentGraphState.CURRENT_STEP_INDEX, 1,
                PatentGraphState.ENVIRONMENT_CHANGED, 1
        ));

        Map<String, Object> out = node.apply(state);

        assertEquals(List.of("retrieval", "analysis", "synthesize"), out.get(PatentGraphState.PLAN));
        assertEquals(1, out.get(PatentGraphState.CURRENT_STEP_INDEX));
        assertEquals(0, out.get(PatentGraphState.NEED_REPLAN));
        assertEquals(0, out.get(PatentGraphState.ENVIRONMENT_CHANGED));
        verify(replanService, never()).replan(eq("用户问题"), anyList());
    }

    @Test
    void replanNode_shouldFullReplanAndTriggerReflectionOnFailure() {
        ReplanService replanService = mock(ReplanService.class);
        ReflectionService reflectionService = mock(ReflectionService.class);
        when(replanService.replan(eq("问题"), anyList())).thenReturn(List.of("retrieval", "synthesize"));
        ReplanNode node = new ReplanNode(replanService);
        ReflectionTestUtils.setField(node, "reflectionService", reflectionService);

        PatentGraphState state = new PatentGraphState(Map.of(
                PatentGraphState.USER_MESSAGE, "问题",
                PatentGraphState.CHAT_ID, "chat-x",
                PatentGraphState.STEP_RESULTS, List.of("结果不足"),
                PatentGraphState.PLAN, List.of("retrieval", "analysis"),
                PatentGraphState.CURRENT_STEP_INDEX, 1,
                PatentGraphState.ENVIRONMENT_CHANGED, 0
        ));

        Map<String, Object> out = node.apply(state);

        assertEquals(List.of("retrieval", "synthesize"), out.get(PatentGraphState.PLAN));
        assertEquals(0, out.get(PatentGraphState.CURRENT_STEP_INDEX));
        verify(reflectionService).reflect("chat-x", "问题", List.of("结果不足"), false);
    }
}

