package com.risk.backend.repository;

import com.risk.backend.domain.Gbt20984RiskMath;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ErmAssessmentRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ErmAssessmentRepository(@Qualifier("riskJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insertAssessment(long tenantId, Long createdByUserId, String title, String framework) {
        KeyHolder kh = new GeneratedKeyHolder();
        MapSqlParameterSource p = new MapSqlParameterSource(Map.of(
                "tenantId", tenantId,
                "title", title,
                "framework", framework == null || framework.isBlank() ? "GB/T 20984" : framework
        ));
        p.addValue("createdBy", createdByUserId);
        jdbc.update("""
                INSERT INTO erm.risk_assessment (tenant_id, title, framework, created_by_user_id)
                VALUES (:tenantId, :title, :framework, :createdBy)
                """, p, kh, new String[]{"id"});
        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("insert assessment failed");
        }
        return key.longValue();
    }

    public List<AssessmentRow> list(long tenantId, int limit, int offset) {
        return jdbc.query("""
                SELECT id, tenant_id, title, status, framework, chat_id, summary,
                high_risk_count, medium_risk_count, low_risk_count, created_at, updated_at
                FROM erm.risk_assessment WHERE tenant_id = :t ORDER BY id DESC LIMIT :lim OFFSET :off
                """, Map.of("t", tenantId, "lim", limit, "off", offset), ASSESSMENT_MAPPER);
    }

    public boolean updateMeta(long tenantId, long id, String status, String summary, String chatId) {
        MapSqlParameterSource p = new MapSqlParameterSource(Map.of("tenantId", tenantId, "id", id));
        p.addValue("status", status);
        p.addValue("summary", summary);
        p.addValue("chatId", chatId);
        return jdbc.update("""
                UPDATE erm.risk_assessment SET
                status = COALESCE(:status, status),
                summary = COALESCE(:summary, summary),
                chat_id = COALESCE(:chatId, chat_id),
                updated_at = NOW()
                WHERE tenant_id = :tenantId AND id = :id
                """, p) == 1;
    }

    public AssessmentRow getRow(long tenantId, long id) {
        var list = jdbc.query("""
                SELECT id, tenant_id, title, status, framework, chat_id, summary,
                high_risk_count, medium_risk_count, low_risk_count, created_at, updated_at
                FROM erm.risk_assessment WHERE tenant_id = :t AND id = :id
                """, Map.of("t", tenantId, "id", id), ASSESSMENT_MAPPER);
        return list.stream().findFirst().orElse(null);
    }

    public AssessmentDetail loadDetail(long tenantId, long id) {
        AssessmentRow row = getRow(tenantId, id);
        if (row == null) {
            return null;
        }
        List<Long> assets = jdbc.query("SELECT asset_id FROM erm.assessment_asset WHERE assessment_id = :aid",
                Map.of("aid", id), (rs, i) -> rs.getLong(1));
        List<Long> threats = jdbc.query("SELECT threat_id FROM erm.assessment_threat WHERE assessment_id = :aid",
                Map.of("aid", id), (rs, i) -> rs.getLong(1));
        List<Long> vulns = jdbc.query("SELECT vulnerability_id FROM erm.assessment_vulnerability WHERE assessment_id = :aid",
                Map.of("aid", id), (rs, i) -> rs.getLong(1));
        List<Long> measures = jdbc.query("SELECT measure_id FROM erm.assessment_measure WHERE assessment_id = :aid",
                Map.of("aid", id), (rs, i) -> rs.getLong(1));
        List<AssessedRiskRow> risks = listAssessedRisks(tenantId, id);
        return new AssessmentDetail(row, assets, threats, vulns, measures, risks);
    }

    public void replaceLinks(long tenantId, long assessmentId, String kind, List<Long> ids) {
        assertAssessmentTenant(tenantId, assessmentId);
        String table = switch (kind) {
            case "asset" -> "assessment_asset";
            case "threat" -> "assessment_threat";
            case "vulnerability" -> "assessment_vulnerability";
            case "measure" -> "assessment_measure";
            default -> throw new IllegalArgumentException(kind);
        };
        String col = switch (kind) {
            case "asset" -> "asset_id";
            case "threat" -> "threat_id";
            case "vulnerability" -> "vulnerability_id";
            case "measure" -> "measure_id";
            default -> throw new IllegalArgumentException(kind);
        };
        jdbc.update("DELETE FROM erm." + table + " WHERE assessment_id = :aid", Map.of("aid", assessmentId));
        if (ids == null || ids.isEmpty()) {
            return;
        }
        String insert = "INSERT INTO erm." + table + " (assessment_id, " + col + ") VALUES (:aid, :rid)";
        for (Long rid : ids) {
            if (rid != null && rid > 0) {
                jdbc.update(insert, Map.of("aid", assessmentId, "rid", rid));
            }
        }
    }

    public long insertAssessedRisk(long tenantId, long assessmentId, String title, int likelihood, int impact,
                                   String notes, String treatment) {
        assertAssessmentTenant(tenantId, assessmentId);
        String level = Gbt20984RiskMath.riskLevel(likelihood, impact);
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO erm.assessed_risk (assessment_id, title, likelihood, impact, risk_level, notes, treatment)
                VALUES (:aid, :title, :likelihood, :impact, :level, :notes, :treatment)
                """, new MapSqlParameterSource(Map.of(
                "aid", assessmentId,
                "title", title == null ? "" : title,
                "likelihood", Gbt20984RiskMath.clamp15(likelihood),
                "impact", Gbt20984RiskMath.clamp15(impact),
                "level", level,
                "notes", notes == null ? "" : notes,
                "treatment", treatment == null ? "" : treatment
        )), kh, new String[]{"id"});
        Number key = kh.getKey();
        if (key == null) {
            throw new IllegalStateException("insert assessed risk failed");
        }
        refreshAggregates(assessmentId);
        return key.longValue();
    }

    public List<AssessedRiskRow> listAssessedRisks(long tenantId, long assessmentId) {
        assertAssessmentTenant(tenantId, assessmentId);
        return jdbc.query("""
                SELECT r.id, r.assessment_id, r.title, r.likelihood, r.impact, r.risk_level, r.notes, r.treatment, r.created_at
                FROM erm.assessed_risk r
                JOIN erm.risk_assessment a ON a.id = r.assessment_id
                WHERE a.tenant_id = :t AND r.assessment_id = :aid ORDER BY r.id
                """, Map.of("t", tenantId, "aid", assessmentId), RISK_MAPPER);
    }

    public boolean deleteAssessedRisk(long tenantId, long riskId) {
        var row = jdbc.query("""
                SELECT r.assessment_id FROM erm.assessed_risk r
                JOIN erm.risk_assessment a ON a.id = r.assessment_id
                WHERE a.tenant_id = :t AND r.id = :rid
                """, Map.of("t", tenantId, "rid", riskId), (rs, i) -> rs.getLong(1));
        if (row.isEmpty()) {
            return false;
        }
        long aid = row.getFirst();
        int n = jdbc.update("DELETE FROM erm.assessed_risk WHERE id = :id", Map.of("id", riskId));
        if (n == 1) {
            refreshAggregates(aid);
        }
        return n == 1;
    }

    private void assertAssessmentTenant(long tenantId, long assessmentId) {
        Integer c = jdbc.queryForObject("""
                SELECT COUNT(*) FROM erm.risk_assessment WHERE tenant_id = :t AND id = :id
                """, Map.of("t", tenantId, "id", assessmentId), Integer.class);
        if (c == null || c == 0) {
            throw new IllegalArgumentException("assessment not found");
        }
    }

    private void refreshAggregates(long assessmentId) {
        jdbc.update("""
                UPDATE erm.risk_assessment a SET
                high_risk_count = (SELECT COUNT(*) FROM erm.assessed_risk r WHERE r.assessment_id = a.id AND r.risk_level = 'HIGH'),
                medium_risk_count = (SELECT COUNT(*) FROM erm.assessed_risk r WHERE r.assessment_id = a.id AND r.risk_level = 'MEDIUM'),
                low_risk_count = (SELECT COUNT(*) FROM erm.assessed_risk r WHERE r.assessment_id = a.id AND r.risk_level = 'LOW'),
                updated_at = NOW()
                WHERE a.id = :id
                """, Map.of("id", assessmentId));
    }

    private static final RowMapper<AssessmentRow> ASSESSMENT_MAPPER = (rs, i) -> new AssessmentRow(
            rs.getLong("id"),
            rs.getLong("tenant_id"),
            rs.getString("title"),
            rs.getString("status"),
            rs.getString("framework"),
            rs.getString("chat_id"),
            rs.getString("summary"),
            rs.getInt("high_risk_count"),
            rs.getInt("medium_risk_count"),
            rs.getInt("low_risk_count"),
            rs.getTimestamp("created_at").toInstant().toString(),
            rs.getTimestamp("updated_at").toInstant().toString()
    );

    private static final RowMapper<AssessedRiskRow> RISK_MAPPER = (rs, i) -> new AssessedRiskRow(
            rs.getLong("id"),
            rs.getLong("assessment_id"),
            rs.getString("title"),
            rs.getInt("likelihood"),
            rs.getInt("impact"),
            rs.getString("risk_level"),
            rs.getString("notes"),
            rs.getString("treatment"),
            rs.getTimestamp("created_at").toInstant().toString()
    );

    public record AssessmentRow(long id, long tenantId, String title, String status, String framework, String chatId,
                                String summary, int highRiskCount, int mediumRiskCount, int lowRiskCount,
                                String createdAt, String updatedAt) {}

    public record AssessedRiskRow(long id, long assessmentId, String title, int likelihood, int impact,
                                  String riskLevel, String notes, String treatment, String createdAt) {}

    public record AssessmentDetail(AssessmentRow assessment, List<Long> assetIds, List<Long> threatIds,
                                   List<Long> vulnerabilityIds, List<Long> measureIds, List<AssessedRiskRow> assessedRisks) {}
}
