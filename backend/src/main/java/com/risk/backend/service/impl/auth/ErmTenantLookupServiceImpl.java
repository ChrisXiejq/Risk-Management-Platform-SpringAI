package com.risk.backend.service.impl.auth;

import com.risk.backend.repository.ErmTenantUserRepository;
import com.risk.backend.repository.ErmTenantUserRepository.TenantRow;
import com.risk.backend.service.intf.ErmTenantLookupService;
import com.risk.backend.service.impl.support.TenantCodeNormalization;
import org.springframework.stereotype.Service;

@Service
public class ErmTenantLookupServiceImpl implements ErmTenantLookupService {

    private final ErmTenantUserRepository repo;

    public ErmTenantLookupServiceImpl(ErmTenantUserRepository repo) {
        this.repo = repo;
    }

    @Override
    public TenantRow tenantByCode(String tenantCode) {
        return repo.findTenantByCode(TenantCodeNormalization.normalizeTenantCode(tenantCode)).orElse(null);
    }
}
