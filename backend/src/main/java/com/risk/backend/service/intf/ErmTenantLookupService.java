package com.risk.backend.service.intf;

import com.risk.backend.repository.ErmTenantUserRepository.TenantRow;

public interface ErmTenantLookupService {

    TenantRow tenantByCode(String tenantCode);
}
