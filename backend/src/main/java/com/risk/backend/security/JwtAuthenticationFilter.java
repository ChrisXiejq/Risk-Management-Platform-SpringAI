package com.risk.backend.security;

import com.risk.backend.ai.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String p = request.getServletPath();
        if (p.startsWith("/actuator/")) {
            return true;
        }
        boolean needJwt = p.startsWith("/erm/")
                || p.startsWith("/risk/")
                || p.equals("/auth/me");
        return !needJwt;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"未登录或令牌缺失\",\"data\":null}");
            return;
        }
        String token = header.substring(7).trim();
        try {
            Claims c = jwtService.parse(token);
            Long tenantId = toLong(c.get("tenantId"));
            String tenantCode = c.get("tenantCode", String.class);
            Long userId = toLong(c.get("userId"));
            String username = c.get("username", String.class);
            String displayName = c.get("displayName", String.class);
            String role = c.get("role", String.class);
            ErmUserDetails details = new ErmUserDetails(tenantId, tenantCode, userId, username, displayName, role);
            var auth = new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            if (tenantCode != null && !tenantCode.isBlank()) {
                TenantContext.setTenantId(tenantCode.trim());
            }
            try {
                filterChain.doFilter(request, response);
            } finally {
                TenantContext.clear();
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"令牌无效或已过期\",\"data\":null}");
        }
    }

    private static Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(v.toString());
    }
}
