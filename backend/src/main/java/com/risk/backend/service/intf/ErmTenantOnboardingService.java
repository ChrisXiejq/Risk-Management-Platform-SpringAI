package com.risk.backend.service.intf;

import com.risk.backend.service.dto.LoginResult;

public interface ErmTenantOnboardingService {

    LoginResult register(String tenantCode, String tenantName, String adminUsername, String adminPassword,
                         String displayName);
}
