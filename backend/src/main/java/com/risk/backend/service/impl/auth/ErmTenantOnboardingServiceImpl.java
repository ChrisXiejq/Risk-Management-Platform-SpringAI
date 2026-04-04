package com.risk.backend.service.impl.auth;

import com.risk.backend.repository.ErmTenantUserRepository;
import com.risk.backend.security.JwtService;
import com.risk.backend.service.intf.ErmTenantOnboardingService;
import com.risk.backend.service.dto.LoginResult;
import com.risk.backend.service.impl.support.TenantCodeNormalization;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ErmTenantOnboardingServiceImpl implements ErmTenantOnboardingService {

    private final ErmTenantUserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ErmTenantOnboardingServiceImpl(ErmTenantUserRepository repo, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional(transactionManager = "riskTransactionManager")
    public LoginResult register(String tenantCode, String tenantName, String adminUsername, String adminPassword,
                                String displayName) {
        String code = TenantCodeNormalization.normalizeTenantCode(tenantCode);
        if (!TenantCodeNormalization.TENANT_CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("租户代码需 2–64 位小写字母、数字、下划线或连字符");
        }
        if (tenantName == null || tenantName.isBlank()) {
            throw new IllegalArgumentException("租户名称不能为空");
        }
        if (adminUsername == null || adminUsername.length() < 2) {
            throw new IllegalArgumentException("用户名至少 2 个字符");
        }
        if (adminPassword == null || adminPassword.length() < 6) {
            throw new IllegalArgumentException("密码至少 6 位");
        }
        if (repo.findTenantByCode(code).isPresent()) {
            throw new IllegalArgumentException("租户代码已存在");
        }
        long tid = repo.insertTenant(code, tenantName.trim());
        String hash = passwordEncoder.encode(adminPassword);
        String dn = displayName == null ? adminUsername : displayName;
        long uid = repo.insertUser(tid, adminUsername.trim(), hash, dn, "ADMIN");
        String token = jwtService.createAccessToken(tid, code, uid, adminUsername.trim(), dn, "ADMIN");
        return new LoginResult(token, code, adminUsername.trim(), dn, "ADMIN");
    }
}
