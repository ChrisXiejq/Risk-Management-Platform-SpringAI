package com.risk.backend.service.impl.identification;

import com.risk.backend.infrastructure.cache.InvalidateDashboardCache;
import com.risk.backend.repository.ErmIdentificationRepository;
import com.risk.backend.repository.ErmIdentificationRepository.AssetRow;
import com.risk.backend.service.intf.ErmAssetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ErmAssetServiceImpl implements ErmAssetService {

    private final ErmIdentificationRepository repo;

    public ErmAssetServiceImpl(ErmIdentificationRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<AssetRow> listAssets(long tenantId) {
        return repo.listAssets(tenantId);
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public long createAsset(long tenantId, String name, String category, Integer criticality, String description,
                            String ownerLabel, String locationLabel) {
        int c = criticality == null ? 3 : criticality;
        return repo.insertAsset(tenantId, name, category, c, description, ownerLabel, locationLabel);
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public void updateAsset(long tenantId, long id, String name, String category, int criticality, String description,
                            String ownerLabel, String locationLabel) {
        if (!repo.updateAsset(tenantId, id, name, category, criticality, description, ownerLabel, locationLabel)) {
            throw new IllegalArgumentException("资产不存在");
        }
    }

    @Override
    @InvalidateDashboardCache
    @Transactional(transactionManager = "riskTransactionManager")
    public void deleteAsset(long tenantId, long id) {
        if (!repo.deleteAsset(tenantId, id)) {
            throw new IllegalArgumentException("资产不存在");
        }
    }
}
