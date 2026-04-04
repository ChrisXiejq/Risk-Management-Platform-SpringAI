package com.risk.backend.infrastructure.cache;

/**
 * Spring Cache 逻辑名（物理键前缀由 {@link CacheConfiguration} 统一加 namespace）。
 */
public final class DashboardCacheNames {

    public static final String OVERVIEW = "erm:dashboard:overview";

    private DashboardCacheNames() {
    }
}
