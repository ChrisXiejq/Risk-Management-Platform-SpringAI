package com.risk.backend.service.intf;

import com.risk.backend.repository.ErmIdentificationRepository.MeasureRow;

import java.util.List;

public interface ErmSecurityMeasureService {

    List<MeasureRow> listMeasures(long tenantId);

    long createMeasure(long tenantId, String name, String measureType, String description, String effectivenessNote);

    void updateMeasure(long tenantId, long id, String name, String measureType, String description, String effectivenessNote);

    void deleteMeasure(long tenantId, long id);
}
