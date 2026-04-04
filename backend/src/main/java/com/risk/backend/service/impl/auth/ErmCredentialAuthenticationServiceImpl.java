package com.risk.backend.service.impl.auth;

import com.risk.backend.repository.ErmTenantUserRepository;
import com.risk.backend.repository.ErmTenantUserRepository.UserAuthRow;
import com.risk.backend.security.JwtService;
import com.risk.backend.service.intf.ErmCredentialAuthenticationService;
import com.risk.backend.service.dto.LoginResult;
import com.risk.backend.service.impl.support.TenantCodeNormalization;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ErmCredentialAuthenticationServiceImpl implements ErmCredentialAuthenticationService {

    private final ErmTenantUserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ErmCredentialAuthenticationServiceImpl(ErmTenantUserRepository repo, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public LoginResult login(String tenantCode, String username, String password) {
        String code = TenantCodeNormalization.normalizeTenantCode(tenantCode);
        UserAuthRow u = repo.findUserForLogin(code, username.trim())
                .orElseThrow(() -> new IllegalArgumentException("租户或用户名/密码错误"));
        if (!passwordEncoder.matches(password, u.passwordHash())) {
            throw new IllegalArgumentException("租户或用户名/密码错误");
        }
        String token = jwtService.createAccessToken(u.tenantId(), u.tenantCode(), u.id(), u.username(), u.displayName(), u.role());
        return new LoginResult(token, u.tenantCode(), u.username(), u.displayName(), u.role());
    }
}
