package com.risk.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class ErmUserDetails implements UserDetails {

    private final Long tenantId;
    private final String tenantCode;
    private final Long userId;
    private final String username;
    private final String displayName;
    private final String role;

    public ErmUserDetails(Long tenantId, String tenantCode, Long userId, String username, String displayName, String role) {
        this.tenantId = tenantId;
        this.tenantCode = tenantCode;
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.role = role == null ? "USER" : role;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
