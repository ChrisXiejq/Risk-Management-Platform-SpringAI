package com.risk.backend.service.intf;

import com.risk.backend.repository.ErmIdentificationRepository.AssetRow;

import java.util.List;

public interface ErmAssetService {

    List<AssetRow> listAssets(long tenantId);

    long createAsset(long tenantId, String name, String category, Integer criticality, String description,
                     String ownerLabel, String locationLabel);

    void updateAsset(long tenantId, long id, String name, String category, int criticality, String description,
                     String ownerLabel, String locationLabel);

    void deleteAsset(long tenantId, long id);
}
