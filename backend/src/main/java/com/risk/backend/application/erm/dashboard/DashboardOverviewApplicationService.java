package com.risk.backend.application.erm.dashboard;

import com.risk.backend.infrastructure.cache.DashboardCacheNames;
import com.risk.backend.service.intf.ErmDashboardService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 应用层：聚合「工作台总览」读模型，并在 Redis（如 Upstash）侧做短期缓存，减轻 Supabase 读压力。
 */
@Service
public class DashboardOverviewApplicationService {

    private final ErmDashboardService ermDashboardService;

    public DashboardOverviewApplicationService(ErmDashboardService ermDashboardService) {
        this.ermDashboardService = ermDashboardService;
    }

    @Cacheable(cacheNames = DashboardCacheNames.OVERVIEW, key = "'t:' + #tenantId")
    public Map<String, Object> overview(long tenantId) {
        return ermDashboardService.overview(tenantId);
    }
}
