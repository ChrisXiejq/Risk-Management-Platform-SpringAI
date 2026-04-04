package com.risk.backend.service.intf;

public interface ErmAssessedRiskService {

    long addRisk(long tenantId, long assessmentId, String title, int likelihood, int impact, String notes, String treatment);

    void deleteRisk(long tenantId, long riskId);
}
