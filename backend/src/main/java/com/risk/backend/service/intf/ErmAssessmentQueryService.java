package com.risk.backend.service.intf;

import com.risk.backend.repository.ErmAssessmentRepository.AssessmentDetail;
import com.risk.backend.repository.ErmAssessmentRepository.AssessmentRow;

import java.util.List;

public interface ErmAssessmentQueryService {

    List<AssessmentRow> list(long tenantId, int page, int size);

    AssessmentDetail detail(long tenantId, long id);
}
