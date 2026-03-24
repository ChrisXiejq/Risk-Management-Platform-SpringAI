package com.inovationbehavior.backend.ai.reflect;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReflectionServiceTest {

    @Test
    void reflect_shouldStoreGeneratedRule() {
        ChatModel chatModel = mock(ChatModel.class);
        ReflectiveRuleStore ruleStore = new InMemoryReflectiveRuleStore();
        ReflectionService service = new ReflectionService(chatModel, ruleStore);

        AssistantMessage assistantMessage = new AssistantMessage("检索失败时先补充公开信息来源");
        ChatResponse response = ChatResponse.builder()
                .generations(List.of(new Generation(assistantMessage)))
                .build();
        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        service.reflect("chat1", "如何评估风险", List.of("暂无证据"), false);

        assertEquals(List.of("检索失败时先补充公开信息来源"), ruleStore.getRecentRules("chat1", 5));
    }

    @Test
    void reflect_shouldIgnoreExceptionAndNotThrow() {
        ChatModel chatModel = mock(ChatModel.class);
        ReflectiveRuleStore ruleStore = mock(ReflectiveRuleStore.class);
        ReflectionService service = new ReflectionService(chatModel, ruleStore);
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("model down"));

        service.reflect("chat1", "q", List.of("r"), true);

        verify(ruleStore, org.mockito.Mockito.never()).addRule(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}

