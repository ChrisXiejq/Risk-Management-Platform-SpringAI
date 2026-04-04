package com.risk.backend.service.intf;

import java.util.List;

public interface ErmAssessmentCommandService {

    long create(long tenantId, Long userId, String title, String framework);

    void updateMeta(long tenantId, long id, String status, String summary, String chatId);

    void setLinks(long tenantId, long assessmentId, List<Long> assetIds, List<Long> threatIds,
                  List<Long> vulnerabilityIds, List<Long> measureIds);
}
