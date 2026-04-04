package com.risk.backend.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class ErmTenantUserRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ErmTenantUserRepository(@Qualifier("riskJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insertTenant(String code, String name) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO erm.tenant (code, name) VALUES (:code, :name)
                """, new MapSqlParameterSource(Map.of("code", code, "name", name)), kh, new String[]{"id"});
        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("insert tenant failed");
        }
        return key.longValue();
    }

    public Optional<TenantRow> findTenantByCode(String code) {
        var list = jdbc.query("""
                SELECT id, code, name FROM erm.tenant WHERE code = :code
                """, Map.of("code", code), TENANT_MAPPER);
        return list.stream().findFirst();
    }

    public long insertUser(long tenantId, String username, String passwordHash, String displayName, String role) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO erm.app_user (tenant_id, username, password_hash, display_name, role)
                VALUES (:tenantId, :username, :passwordHash, :displayName, :role)
                """, new MapSqlParameterSource(Map.of(
                "tenantId", tenantId,
                "username", username,
                "passwordHash", passwordHash,
                "displayName", displayName == null ? "" : displayName,
                "role", role == null ? "USER" : role
        )), kh, new String[]{"id"});
        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("insert user failed");
        }
        return key.longValue();
    }

    public Optional<UserAuthRow> findUserForLogin(String tenantCode, String username) {
        var list = jdbc.query("""
                SELECT u.id, u.tenant_id, u.username, u.password_hash, u.display_name, u.role, t.code AS tenant_code
                FROM erm.app_user u
                JOIN erm.tenant t ON t.id = u.tenant_id
                WHERE t.code = :tenantCode AND u.username = :username
                """, Map.of("tenantCode", tenantCode, "username", username), USER_AUTH_MAPPER);
        return list.stream().findFirst();
    }

    private static final RowMapper<TenantRow> TENANT_MAPPER = (rs, i) -> new TenantRow(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name")
    );

    private static final RowMapper<UserAuthRow> USER_AUTH_MAPPER = (rs, i) -> new UserAuthRow(
            rs.getLong("id"),
            rs.getLong("tenant_id"),
            rs.getString("tenant_code"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getString("display_name"),
            rs.getString("role")
    );

    public record TenantRow(long id, String code, String name) {}

    public record UserAuthRow(long id, long tenantId, String tenantCode, String username, String passwordHash,
                              String displayName, String role) {}
}
