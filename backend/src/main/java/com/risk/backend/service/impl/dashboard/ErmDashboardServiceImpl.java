package com.risk.backend.service.impl.dashboard;

import com.risk.backend.repository.ErmDashboardRepository;
import com.risk.backend.repository.ErmDashboardRepository.OverviewRow;
import com.risk.backend.repository.ErmDashboardRepository.RecentAssessmentRow;
import com.risk.backend.service.intf.ErmDashboardService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ErmDashboardServiceImpl implements ErmDashboardService {

    private final ErmDashboardRepository repo;

    public ErmDashboardServiceImpl(ErmDashboardRepository repo) {
        this.repo = repo;
    }

    @Override
    public Map<String, Object> overview(long tenantId) {
        OverviewRow o = repo.overview(tenantId);
        List<RecentAssessmentRow> recent = repo.recentAssessments(tenantId, 8);
        Map<String, Object> m = new HashMap<>();
        m.put("counts", o);
        m.put("recentAssessments", recent);
        m.put("hints", List.of(
                "风险信息总揽：汇总识别要素规模、评估活动状态与矩阵中高中低分布（按各评估记录汇总）。",
                "识别阶段对应 GB/T 20984 中的资产、威胁、脆弱性、已有安全措施。",
                "完成识别后，在「评估记录」中关联要素并录入 Likelihood/Impact，系统自动映射风险等级。",
                "Agent 智能分析请使用评估详情中的对话区，chatId 与评估绑定便于审计与追溯。"
        ));
        return m;
    }
}
