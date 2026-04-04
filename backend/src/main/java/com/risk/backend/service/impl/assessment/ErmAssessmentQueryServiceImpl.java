package com.risk.backend.service.impl.assessment;

import com.risk.backend.repository.ErmAssessmentRepository;
import com.risk.backend.repository.ErmAssessmentRepository.AssessmentDetail;
import com.risk.backend.repository.ErmAssessmentRepository.AssessmentRow;
import com.risk.backend.service.intf.ErmAssessmentQueryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ErmAssessmentQueryServiceImpl implements ErmAssessmentQueryService {

    private final ErmAssessmentRepository repo;

    public ErmAssessmentQueryServiceImpl(ErmAssessmentRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<AssessmentRow> list(long tenantId, int page, int size) {
        int lim = Math.min(Math.max(size, 1), 100);
        int off = Math.max(page, 0) * lim;
        return repo.list(tenantId, lim, off);
    }

    @Override
    public AssessmentDetail detail(long tenantId, long id) {
        AssessmentDetail d = repo.loadDetail(tenantId, id);
        if (d == null) {
            throw new IllegalArgumentException("评估记录不存在");
        }
        return d;
    }
}
