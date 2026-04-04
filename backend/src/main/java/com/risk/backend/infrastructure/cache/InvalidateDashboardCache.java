package com.risk.backend.infrastructure.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记「会改变工作台统计口径」的写操作，由 AOP 驱逐 {@link DashboardCacheNames#OVERVIEW}。
 * 首个参数须为租户 {@code tenantId}（long）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface InvalidateDashboardCache {
}
