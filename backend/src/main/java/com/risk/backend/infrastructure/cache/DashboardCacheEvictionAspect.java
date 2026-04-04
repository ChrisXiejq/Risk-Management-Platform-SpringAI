package com.risk.backend.infrastructure.cache;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.cache.CacheManager;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * 写路径统一失效仪表盘缓存，避免在多个 Service 中重复书写 {@code @CacheEvict}。
 */
@Aspect
@Component
public class DashboardCacheEvictionAspect {

    private final CacheManager cacheManager;

    public DashboardCacheEvictionAspect(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @AfterReturning("@annotation(com.risk.backend.infrastructure.cache.InvalidateDashboardCache)")
    public void evictOverview(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0 || !(args[0] instanceof Number)) {
            return;
        }
        long tenantId = ((Number) args[0]).longValue();
        var cache = cacheManager.getCache(DashboardCacheNames.OVERVIEW);
        if (cache != null) {
            cache.evict(cacheKey(tenantId));
        }
    }

    private static @NonNull String cacheKey(long tenantId) {
        return "t:" + tenantId;
    }
}
