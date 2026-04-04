package com.risk.backend.security;

import com.risk.backend.config.ErmProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtService {

    private final ErmProperties props;

    public JwtService(ErmProperties props) {
        this.props = props;
    }

    public String createAccessToken(Long tenantId, String tenantCode, Long userId, String username, String displayName, String role) {
        long now = System.currentTimeMillis();
        int hours = Math.max(1, props.getJwt().getAccessHours());
        Date exp = new Date(now + hours * 3600_000L);
        return Jwts.builder()
                .claims(Map.of(
                        "tenantId", tenantId,
                        "tenantCode", tenantCode,
                        "userId", userId,
                        "username", username,
                        "displayName", displayName == null ? "" : displayName,
                        "role", role == null ? "USER" : role
                ))
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(exp)
                .signWith(signingKey())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        String secret = props.getJwt().getSecret();
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("app.erm.jwt.secret must be at least 32 characters");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
