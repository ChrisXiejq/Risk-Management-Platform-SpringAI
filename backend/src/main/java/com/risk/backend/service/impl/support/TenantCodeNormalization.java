package com.risk.backend.service.impl.support;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 租户代码校验与规范化（供注册、登录等实现类复用）。
 */
public final class TenantCodeNormalization {

    public static final Pattern TENANT_CODE_PATTERN =
            Pattern.compile("^[a-z0-9][a-z0-9_-]{1,62}[a-z0-9]$|^[a-z0-9]{2}$");

    private TenantCodeNormalization() {
    }

    public static String normalizeTenantCode(String tenantCode) {
        if (tenantCode == null) {
            return "";
        }
        return tenantCode.trim().toLowerCase(Locale.ROOT);
    }
}
