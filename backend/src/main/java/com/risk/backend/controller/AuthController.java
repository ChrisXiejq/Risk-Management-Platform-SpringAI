package com.risk.backend.controller;

import com.risk.backend.security.ErmUserDetails;
import com.risk.backend.service.intf.ErmCredentialAuthenticationService;
import com.risk.backend.service.intf.ErmTenantOnboardingService;
import com.risk.backend.service.dto.LoginResult;
import com.risk.backend.model.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final ErmTenantOnboardingService tenantOnboardingService;
    private final ErmCredentialAuthenticationService credentialAuthenticationService;

    public AuthController(ErmTenantOnboardingService tenantOnboardingService,
                          ErmCredentialAuthenticationService credentialAuthenticationService) {
        this.tenantOnboardingService = tenantOnboardingService;
        this.credentialAuthenticationService = credentialAuthenticationService;
    }

    @PostMapping("/register")
    public Result register(@Valid @RequestBody RegisterBody body) {
        try {
            LoginResult r = tenantOnboardingService.register(body.tenantCode(), body.tenantName(), body.adminUsername(),
                    body.adminPassword(), body.displayName());
            return Result.success(Map.of(
                    "token", r.token(),
                    "tenantCode", r.tenantCode(),
                    "username", r.username(),
                    "displayName", r.displayName(),
                    "role", r.role()
            ));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            return Result.error("租户或用户名已存在");
        }
    }

    @PostMapping("/login")
    public Result login(@Valid @RequestBody LoginBody body) {
        try {
            LoginResult r = credentialAuthenticationService.login(body.tenantCode(), body.username(), body.password());
            return Result.success(Map.of(
                    "token", r.token(),
                    "tenantCode", r.tenantCode(),
                    "username", r.username(),
                    "displayName", r.displayName(),
                    "role", r.role()
            ));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/me")
    public Result me(@AuthenticationPrincipal ErmUserDetails user) {
        if (user == null) {
            return Result.error("未登录");
        }
        return Result.success(Map.of(
                "tenantId", user.getTenantId(),
                "tenantCode", user.getTenantCode(),
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "displayName", user.getDisplayName(),
                "role", user.getRole()
        ));
    }

    public record RegisterBody(
            @NotBlank String tenantCode,
            @NotBlank String tenantName,
            @NotBlank String adminUsername,
            @NotBlank String adminPassword,
            String displayName
    ) {}

    public record LoginBody(
            @NotBlank String tenantCode,
            @NotBlank String username,
            @NotBlank String password
    ) {}
}
