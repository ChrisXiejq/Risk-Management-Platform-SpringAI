package com.risk.backend.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ErmIdentificationRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ErmIdentificationRepository(@Qualifier("riskJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // --- assets ---
    public List<AssetRow> listAssets(long tenantId) {
        return jdbc.query("""
                SELECT id, tenant_id, name, category, criticality, description, owner_label, location_label, created_at, updated_at
                FROM erm.risk_asset WHERE tenant_id = :t ORDER BY id DESC
                """, Map.of("t", tenantId), ASSET_MAPPER);
    }

    public Optional<AssetRow> findAsset(long tenantId, long id) {
        var q = jdbc.query("""
                SELECT id, tenant_id, name, category, criticality, description, owner_label, location_label, created_at, updated_at
                FROM erm.risk_asset WHERE tenant_id = :t AND id = :id
                """, Map.of("t", tenantId, "id", id), ASSET_MAPPER);
        return q.stream().findFirst();
    }

    public long insertAsset(long tenantId, String name, String category, int criticality, String description,
                            String ownerLabel, String locationLabel) {
        return insertReturningId("""
                INSERT INTO erm.risk_asset (tenant_id, name, category, criticality, description, owner_label, location_label)
                VALUES (:tenantId, :name, :category, :criticality, :description, :ownerLabel, :locationLabel)
                """, Map.of("tenantId", tenantId, "name", name, "category", nullToEmpty(category),
                "criticality", criticality, "description", nullToEmpty(description),
                "ownerLabel", nullToEmpty(ownerLabel), "locationLabel", nullToEmpty(locationLabel)));
    }

    public boolean updateAsset(long tenantId, long id, String name, String category, int criticality, String description,
                               String ownerLabel, String locationLabel) {
        int n = jdbc.update("""
                UPDATE erm.risk_asset SET name=:name, category=:category, criticality=:criticality, description=:description,
                owner_label=:ownerLabel, location_label=:locationLabel, updated_at=NOW()
                WHERE tenant_id=:tenantId AND id=:id
                """, Map.of("tenantId", tenantId, "id", id, "name", name, "category", nullToEmpty(category),
                "criticality", criticality, "description", nullToEmpty(description),
                "ownerLabel", nullToEmpty(ownerLabel), "locationLabel", nullToEmpty(locationLabel)));
        return n == 1;
    }

    public boolean deleteAsset(long tenantId, long id) {
        return jdbc.update("DELETE FROM erm.risk_asset WHERE tenant_id=:t AND id=:id", Map.of("t", tenantId, "id", id)) == 1;
    }

    // --- threats ---
    public List<ThreatRow> listThreats(long tenantId) {
        return jdbc.query("""
                SELECT id, tenant_id, name, category, description, source_label, created_at, updated_at
                FROM erm.risk_threat WHERE tenant_id = :t ORDER BY id DESC
                """, Map.of("t", tenantId), THREAT_MAPPER);
    }

    public long insertThreat(long tenantId, String name, String category, String description, String sourceLabel) {
        return insertReturningId("""
                INSERT INTO erm.risk_threat (tenant_id, name, category, description, source_label)
                VALUES (:tenantId, :name, :category, :description, :sourceLabel)
                """, Map.of("tenantId", tenantId, "name", name, "category", nullToEmpty(category),
                "description", nullToEmpty(description), "sourceLabel", nullToEmpty(sourceLabel)));
    }

    public boolean updateThreat(long tenantId, long id, String name, String category, String description, String sourceLabel) {
        return jdbc.update("""
                UPDATE erm.risk_threat SET name=:name, category=:category, description=:description,
                source_label=:sourceLabel, updated_at=NOW()
                WHERE tenant_id=:tenantId AND id=:id
                """, Map.of("tenantId", tenantId, "id", id, "name", name, "category", nullToEmpty(category),
                "description", nullToEmpty(description), "sourceLabel", nullToEmpty(sourceLabel))) == 1;
    }

    public boolean deleteThreat(long tenantId, long id) {
        return jdbc.update("DELETE FROM erm.risk_threat WHERE tenant_id=:t AND id=:id", Map.of("t", tenantId, "id", id)) == 1;
    }

    // --- vulnerabilities ---
    public List<VulnRow> listVulns(long tenantId) {
        return jdbc.query("""
                SELECT id, tenant_id, name, severity, description, related_asset_id, created_at, updated_at
                FROM erm.risk_vulnerability WHERE tenant_id = :t ORDER BY id DESC
                """, Map.of("t", tenantId), VULN_MAPPER);
    }

    public long insertVuln(long tenantId, String name, String severity, String description, Long relatedAssetId) {
        MapSqlParameterSource p = new MapSqlParameterSource(Map.of(
                "tenantId", tenantId,
                "name", name,
                "severity", nullToEmpty(severity),
                "description", nullToEmpty(description)
        ));
        p.addValue("relatedAssetId", relatedAssetId);
        return insertReturningId("""
                INSERT INTO erm.risk_vulnerability (tenant_id, name, severity, description, related_asset_id)
                VALUES (:tenantId, :name, :severity, :description, :relatedAssetId)
                """, p);
    }

    public boolean updateVuln(long tenantId, long id, String name, String severity, String description, Long relatedAssetId) {
        MapSqlParameterSource p = new MapSqlParameterSource(Map.of(
                "tenantId", tenantId, "id", id, "name", name,
                "severity", nullToEmpty(severity), "description", nullToEmpty(description)
        ));
        p.addValue("relatedAssetId", relatedAssetId);
        return jdbc.update("""
                UPDATE erm.risk_vulnerability SET name=:name, severity=:severity, description=:description,
                related_asset_id=:relatedAssetId, updated_at=NOW()
                WHERE tenant_id=:tenantId AND id=:id
                """, p) == 1;
    }

    public boolean deleteVuln(long tenantId, long id) {
        return jdbc.update("DELETE FROM erm.risk_vulnerability WHERE tenant_id=:t AND id=:id", Map.of("t", tenantId, "id", id)) == 1;
    }

    // --- measures ---
    public List<MeasureRow> listMeasures(long tenantId) {
        return jdbc.query("""
                SELECT id, tenant_id, name, measure_type, description, effectiveness_note, created_at, updated_at
                FROM erm.security_measure WHERE tenant_id = :t ORDER BY id DESC
                """, Map.of("t", tenantId), MEASURE_MAPPER);
    }

    public long insertMeasure(long tenantId, String name, String measureType, String description, String effectivenessNote) {
        return insertReturningId("""
                INSERT INTO erm.security_measure (tenant_id, name, measure_type, description, effectiveness_note)
                VALUES (:tenantId, :name, :measureType, :description, :effectivenessNote)
                """, Map.of("tenantId", tenantId, "name", name, "measureType", nullToEmpty(measureType),
                "description", nullToEmpty(description), "effectivenessNote", nullToEmpty(effectivenessNote)));
    }

    public boolean updateMeasure(long tenantId, long id, String name, String measureType, String description, String effectivenessNote) {
        return jdbc.update("""
                UPDATE erm.security_measure SET name=:name, measure_type=:measureType, description=:description,
                effectiveness_note=:effectivenessNote, updated_at=NOW()
                WHERE tenant_id=:tenantId AND id=:id
                """, Map.of("tenantId", tenantId, "id", id, "name", name, "measureType", nullToEmpty(measureType),
                "description", nullToEmpty(description), "effectivenessNote", nullToEmpty(effectivenessNote))) == 1;
    }

    public boolean deleteMeasure(long tenantId, long id) {
        return jdbc.update("DELETE FROM erm.security_measure WHERE tenant_id=:t AND id=:id", Map.of("t", tenantId, "id", id)) == 1;
    }

    private long insertReturningId(String sql, MapSqlParameterSource params) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(sql, params, kh, new String[]{"id"});
        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("insert failed");
        }
        return key.longValue();
    }

    private long insertReturningId(String sql, Map<String, Object> params) {
        return insertReturningId(sql, new MapSqlParameterSource(params));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static final RowMapper<AssetRow> ASSET_MAPPER = (rs, i) -> new AssetRow(
            rs.getLong("id"),
            rs.getLong("tenant_id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getInt("criticality"),
            rs.getString("description"),
            rs.getString("owner_label"),
            rs.getString("location_label"),
            rs.getTimestamp("created_at").toInstant().toString(),
            rs.getTimestamp("updated_at").toInstant().toString()
    );

    private static final RowMapper<ThreatRow> THREAT_MAPPER = (rs, i) -> new ThreatRow(
            rs.getLong("id"),
            rs.getLong("tenant_id"),
            rs.getString("name"),
            rs.getString("category"),
            rs.getString("description"),
            rs.getString("source_label"),
            rs.getTimestamp("created_at").toInstant().toString(),
            rs.getTimestamp("updated_at").toInstant().toString()
    );

    private static final RowMapper<VulnRow> VULN_MAPPER = (rs, i) -> new VulnRow(
            rs.getLong("id"),
            rs.getLong("tenant_id"),
            rs.getString("name"),
            rs.getString("severity"),
            rs.getString("description"),
            rs.getObject("related_asset_id", Long.class),
            rs.getTimestamp("created_at").toInstant().toString(),
            rs.getTimestamp("updated_at").toInstant().toString()
    );

    private static final RowMapper<MeasureRow> MEASURE_MAPPER = (rs, i) -> new MeasureRow(
            rs.getLong("id"),
            rs.getLong("tenant_id"),
            rs.getString("name"),
            rs.getString("measure_type"),
            rs.getString("description"),
            rs.getString("effectiveness_note"),
            rs.getTimestamp("created_at").toInstant().toString(),
            rs.getTimestamp("updated_at").toInstant().toString()
    );

    public record AssetRow(long id, long tenantId, String name, String category, int criticality, String description,
                           String ownerLabel, String locationLabel, String createdAt, String updatedAt) {}

    public record ThreatRow(long id, long tenantId, String name, String category, String description, String sourceLabel,
                            String createdAt, String updatedAt) {}

    public record VulnRow(long id, long tenantId, String name, String severity, String description, Long relatedAssetId,
                          String createdAt, String updatedAt) {}

    public record MeasureRow(long id, long tenantId, String name, String measureType, String description,
                             String effectivenessNote, String createdAt, String updatedAt) {}
}
