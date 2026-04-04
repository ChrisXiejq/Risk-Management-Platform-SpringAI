package com.risk.backend.service.impl.identification;

import com.risk.backend.infrastructure.cache.InvalidateDashboardCache;
import com.risk.backend.repository.ErmIdentificationRepository;
import com.risk.backend.repository.ErmIdentificationRepository.MeasureRow;
import com.risk.backend.service.intf.ErmSecurityMeasureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ErmSecurityMeasureServiceImpl implements ErmSecurityMeasureService {

    private final ErmIdentificationRepository repo;

    public ErmSecurityMeasureServiceImpl(ErmIdentificationRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<MeasureRow> listMeasures(long tenantId) {
        return repo.listMeasures(tenantId);
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public long createMeasure(long tenantId, String name, String measureType, String description, String effectivenessNote) {
        return repo.insertMeasure(tenantId, name, measureType, description, effectivenessNote);
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public void updateMeasure(long tenantId, long id, String name, String measureType, String description, String effectivenessNote) {
        if (!repo.updateMeasure(tenantId, id, name, measureType, description, effectivenessNote)) {
            throw new IllegalArgumentException("安全措施不存在");
        }
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public void deleteMeasure(long tenantId, long id) {
        if (!repo.deleteMeasure(tenantId, id)) {
            throw new IllegalArgumentException("安全措施不存在");
        }
    }
}
