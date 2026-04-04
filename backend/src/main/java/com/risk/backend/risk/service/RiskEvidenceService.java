package com.risk.backend.risk.service;

import com.risk.backend.risk.model.RiskEvidence;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MVP：用内存维护每个 chatId 的证据片段。
 * 目前用简易“关键词匹配”做搜索；可后续升级为向量检索/混合检索。
 */
@Service
public class RiskEvidenceService {

    private final Map<String, List<RiskEvidence>> evidenceByChatId = new ConcurrentHashMap<>();

    public RiskEvidence addEvidence(String chatId,
                                     String evidenceType,
                                     String content,
                                     List<String> sources,
                                     Map<String, Object> metadata) {
        if (chatId == null || chatId.isBlank()) throw new IllegalArgumentException("chatId is required");
        RiskEvidence ev = new RiskEvidence(
                UUID.randomUUID().toString(),
                chatId,
                evidenceType,
                content,
                sources == null ? List.of() : sources,
                metadata == null ? Map.of() : metadata
        );
        evidenceByChatId.compute(chatId, (k, list) -> {
            if (list == null) list = new ArrayList<>();
            list.add(ev);
            // 控制内存膨胀：只保留最近 N 条
            int max = 200;
            if (list.size() > max) list = new ArrayList<>(list.subList(list.size() - max, list.size()));
            return list;
        });
        return ev;
    }

    public List<RiskEvidence> search(String chatId, String query, int topK) {
        if (chatId == null || chatId.isBlank()) return List.of();
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isBlank()) return recent(chatId, topK);

        List<RiskEvidence> all = evidenceByChatId.getOrDefault(chatId, List.of());
        if (all.isEmpty()) return List.of();

        // 简易评分：content/sources/evidenceType 的关键词命中数量
        List<String> keywords = List.of(q.split("[\\s,，]+"))
                .stream().filter(s -> !s.isBlank()).collect(Collectors.toList());
        return all.stream()
                .map(ev -> new Object[]{ev, score(ev, keywords)})
                .sorted(Comparator.<Object[]>comparingInt(a -> (int) a[1]).reversed())
                .filter(a -> (int) a[1] > 0)
                .limit(Math.max(1, topK))
                .map(a -> (RiskEvidence) a[0])
                .toList();
    }

    public List<RiskEvidence> recent(String chatId, int topK) {
        List<RiskEvidence> all = evidenceByChatId.getOrDefault(chatId, List.of());
        if (all.isEmpty()) return List.of();
        int n = Math.min(Math.max(1, topK), all.size());
        return new ArrayList<>(all.subList(all.size() - n, all.size()));
    }

    public List<RiskEvidence> getAll(String chatId) {
        return evidenceByChatId.getOrDefault(chatId, List.of());
    }

    public void clear(String chatId) {
        if (chatId == null || chatId.isBlank()) return;
        evidenceByChatId.remove(chatId);
    }

    private int score(RiskEvidence ev, List<String> keywords) {
        int s = 0;
        String content = ev.content() == null ? "" : ev.content().toLowerCase();
        String type = ev.evidenceType() == null ? "" : ev.evidenceType().toLowerCase();
        String sources = ev.sources() == null ? "" : String.join(" ", ev.sources()).toLowerCase();
        for (String k : keywords) {
            if (k.length() < 2) continue;
            if (content.contains(k)) s += 2;
            if (type.contains(k)) s += 1;
            if (sources.contains(k)) s += 1;
        }
        return s;
    }
}

