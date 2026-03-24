package com.inovationbehavior.backend.ai.memory;

import com.inovationbehavior.backend.ai.memory.compression.SummaryCompressor;
import com.inovationbehavior.backend.ai.memory.experiential.ExperientialMemoryService;
import com.inovationbehavior.backend.ai.memory.extraction.AtomicFactExtractor;
import com.inovationbehavior.backend.ai.memory.importance.ImportanceScorer;
import com.inovationbehavior.backend.ai.memory.longterm.LongTermMemoryService;
import com.inovationbehavior.backend.ai.memory.nli.NliConflictDetector;
import com.inovationbehavior.backend.ai.memory.working.WorkingMemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemoryArchitectureTest {

    @Test
    void workingMemory_shouldPruneAndCompressByWindowAndImportance() {
        ImportanceScorer scorer = (u, a, idx) -> idx == 0 ? 0.05 : 0.9;
        SummaryCompressor compressor = (prev, turns) -> "summary-" + turns.size();
        WorkingMemoryService service = new WorkingMemoryService(scorer, compressor, 2, 10, 0.1, 1);

        service.addTurn("c1", "low-importance-message", "assistant");
        var compressed = service.addTurn("c1", "high1", "a1");
        service.addTurn("c1", "high2", "a2");

        String context = service.getContextForPrompt("c1");
        assertTrue(context.contains("high1") || context.contains("high2"));
        assertFalse(context.contains("low-importance-message"));
        assertTrue(compressed.isPresent() || context.contains("此前对话摘要"));
    }

    @Test
    void experientialMemory_shouldRetrieveAndFormatWithDecayOrdering() {
        VectorStore vectorStore = mock(VectorStore.class);
        ExperientialMemoryService service = new ExperientialMemoryService();
        ReflectionTestUtils.setField(service, "experientialVectorStore", vectorStore);
        ReflectionTestUtils.setField(service, "topK", 2);
        ReflectionTestUtils.setField(service, "similarityThreshold", 0.1d);
        ReflectionTestUtils.setField(service, "decayWeightDefault", 1.0d);

        Document olderHighWeight = new Document("old high", java.util.Map.of("decay_weight", "2.0", "created_at", "1000"));
        Document recentLowWeight = new Document("recent low", java.util.Map.of("decay_weight", "0.5", "created_at", "2000"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(recentLowWeight, olderHighWeight));

        List<Document> retrieved = service.retrieve("query", "c1");
        assertTrue(retrieved.size() <= 2);
        assertTrue(retrieved.get(0).getText().contains("old high"));

        String promptBlock = service.formatMemoriesForPrompt(retrieved);
        assertTrue(promptBlock.contains("中期记忆"));
    }

    @Test
    void longTermMemory_shouldSkipWhenNliConflictDetected() {
        VectorStore vectorStore = mock(VectorStore.class);
        NliConflictDetector conflictDetector = (newContent, existing) -> true;
        AtomicFactExtractor factExtractor = mock(AtomicFactExtractor.class);
        LongTermMemoryService service = new LongTermMemoryService(conflictDetector, factExtractor);

        ReflectionTestUtils.setField(service, "memoryVectorStore", vectorStore);
        ReflectionTestUtils.setField(service, "importanceThreshold", 0.2d);
        ReflectionTestUtils.setField(service, "duplicateSimilarityThreshold", 0.99d);
        ReflectionTestUtils.setField(service, "similarityThreshold", 0.1d);
        ReflectionTestUtils.setField(service, "nliCheckTopK", 3);
        ReflectionTestUtils.setField(service, "useAtomicFacts", false);

        Document similar = new Document("existing", java.util.Map.of("id", "doc-1"));
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of())     // dedup
                .thenReturn(List.of(similar)); // nli recall

        service.storeIfEligible("c1", "user", "assistant", 0.9, 1);

        verify(vectorStore).delete(List.of("doc-1"));
        verify(vectorStore).add(any());
        verify(factExtractor, never()).extractFacts(any(), any());
    }
}

