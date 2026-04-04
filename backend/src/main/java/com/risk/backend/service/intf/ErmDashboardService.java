package com.risk.backend.service.intf;

import java.util.Map;

public interface ErmDashboardService {

    Map<String, Object> overview(long tenantId);
}
