package com.risk.backend.service.impl.assessment;

import com.risk.backend.infrastructure.cache.InvalidateDashboardCache;
import com.risk.backend.repository.ErmAssessmentRepository;
import com.risk.backend.service.intf.ErmAssessedRiskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ErmAssessedRiskServiceImpl implements ErmAssessedRiskService {

    private final ErmAssessmentRepository repo;

    public ErmAssessedRiskServiceImpl(ErmAssessmentRepository repo) {
        this.repo = repo;
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public long addRisk(long tenantId, long assessmentId, String title, int likelihood, int impact, String notes, String treatment) {
        return repo.insertAssessedRisk(tenantId, assessmentId, title, likelihood, impact, notes, treatment);
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public void deleteRisk(long tenantId, long riskId) {
        if (!repo.deleteAssessedRisk(tenantId, riskId)) {
            throw new IllegalArgumentException("风险项不存在");
        }
    }
}
