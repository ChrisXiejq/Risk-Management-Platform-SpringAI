package com.risk.backend.controller;

import com.risk.backend.application.erm.dashboard.DashboardOverviewApplicationService;
import com.risk.backend.security.ErmUserDetails;
import com.risk.backend.model.Result;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/erm/dashboard")
public class ErmDashboardController {

    private final DashboardOverviewApplicationService dashboardOverviewApplicationService;

    public ErmDashboardController(DashboardOverviewApplicationService dashboardOverviewApplicationService) {
        this.dashboardOverviewApplicationService = dashboardOverviewApplicationService;
    }

    @GetMapping("/overview")
    public Result overview(@AuthenticationPrincipal ErmUserDetails user) {
        return Result.success(dashboardOverviewApplicationService.overview(user.getTenantId()));
    }
}
