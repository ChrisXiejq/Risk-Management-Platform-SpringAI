package com.risk.backend.service.intf;

import com.risk.backend.repository.ErmIdentificationRepository.ThreatRow;

import java.util.List;

public interface ErmThreatService {

    List<ThreatRow> listThreats(long tenantId);

    long createThreat(long tenantId, String name, String category, String description, String sourceLabel);

    void updateThreat(long tenantId, long id, String name, String category, String description, String sourceLabel);

    void deleteThreat(long tenantId, long id);
}
