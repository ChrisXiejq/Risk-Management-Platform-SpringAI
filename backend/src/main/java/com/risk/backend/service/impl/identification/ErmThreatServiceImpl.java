package com.risk.backend.service.impl.identification;

import com.risk.backend.infrastructure.cache.InvalidateDashboardCache;
import com.risk.backend.repository.ErmIdentificationRepository;
import com.risk.backend.repository.ErmIdentificationRepository.ThreatRow;
import com.risk.backend.service.intf.ErmThreatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ErmThreatServiceImpl implements ErmThreatService {

    private final ErmIdentificationRepository repo;

    public ErmThreatServiceImpl(ErmIdentificationRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<ThreatRow> listThreats(long tenantId) {
        return repo.listThreats(tenantId);
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public long createThreat(long tenantId, String name, String category, String description, String sourceLabel) {
        return repo.insertThreat(tenantId, name, category, description, sourceLabel);
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public void updateThreat(long tenantId, long id, String name, String category, String description, String sourceLabel) {
        if (!repo.updateThreat(tenantId, id, name, category, description, sourceLabel)) {
            throw new IllegalArgumentException("威胁不存在");
        }
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public void deleteThreat(long tenantId, long id) {
        if (!repo.deleteThreat(tenantId, id)) {
            throw new IllegalArgumentException("威胁不存在");
        }
    }
}
