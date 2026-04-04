package com.risk.backend.service.dto;

/**
 * 登录/注册成功后返回给前端的令牌与主体信息。
 */
public record LoginResult(
        String token,
        String tenantCode,
        String username,
        String displayName,
        String role
) {}
