package com.risk.backend.service.impl.assessment;

import com.risk.backend.infrastructure.cache.InvalidateDashboardCache;
import com.risk.backend.repository.ErmAssessmentRepository;
import com.risk.backend.service.intf.ErmAssessmentCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ErmAssessmentCommandServiceImpl implements ErmAssessmentCommandService {

    private final ErmAssessmentRepository repo;

    public ErmAssessmentCommandServiceImpl(ErmAssessmentRepository repo) {
        this.repo = repo;
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public long create(long tenantId, Long userId, String title, String framework) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("标题不能为空");
        }
        return repo.insertAssessment(tenantId, userId, title.trim(), framework);
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public void updateMeta(long tenantId, long id, String status, String summary, String chatId) {
        if (!repo.updateMeta(tenantId, id, status, summary, chatId)) {
            throw new IllegalArgumentException("评估记录不存在");
        }
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public void setLinks(long tenantId, long assessmentId, List<Long> assetIds, List<Long> threatIds,
                         List<Long> vulnerabilityIds, List<Long> measureIds) {
        if (assetIds != null) {
            repo.replaceLinks(tenantId, assessmentId, "asset", assetIds);
        }
        if (threatIds != null) {
            repo.replaceLinks(tenantId, assessmentId, "threat", threatIds);
        }
        if (vulnerabilityIds != null) {
            repo.replaceLinks(tenantId, assessmentId, "vulnerability", vulnerabilityIds);
        }
        if (measureIds != null) {
            repo.replaceLinks(tenantId, assessmentId, "measure", measureIds);
        }
    }
}
