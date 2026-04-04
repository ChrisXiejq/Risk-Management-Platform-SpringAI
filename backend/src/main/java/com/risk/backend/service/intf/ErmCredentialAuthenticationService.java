package com.risk.backend.service.intf;

import com.risk.backend.service.dto.LoginResult;

public interface ErmCredentialAuthenticationService {

    LoginResult login(String tenantCode, String username, String password);
}
