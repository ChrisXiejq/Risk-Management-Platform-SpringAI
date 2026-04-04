package com.risk.backend.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ErmDashboardRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ErmDashboardRepository(@Qualifier("riskJdbcTemplate") NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public OverviewRow overview(long tenantId) {
        Map<String, Object> m = jdbc.queryForMap("""
                SELECT
                  (SELECT COUNT(*) FROM erm.risk_asset WHERE tenant_id = :t) AS assets,
                  (SELECT COUNT(*) FROM erm.risk_threat WHERE tenant_id = :t) AS threats,
                  (SELECT COUNT(*) FROM erm.risk_vulnerability WHERE tenant_id = :t) AS vulnerabilities,
                  (SELECT COUNT(*) FROM erm.security_measure WHERE tenant_id = :t) AS measures,
                  (SELECT COUNT(*) FROM erm.risk_assessment WHERE tenant_id = :t) AS assessments,
                  (SELECT COUNT(*) FROM erm.risk_assessment WHERE tenant_id = :t AND status = 'DRAFT') AS assessments_draft,
                  (SELECT COUNT(*) FROM erm.risk_assessment WHERE tenant_id = :t AND status = 'IN_PROGRESS') AS assessments_progress,
                  (SELECT COUNT(*) FROM erm.risk_assessment WHERE tenant_id = :t AND status = 'COMPLETED') AS assessments_done,
                  (SELECT COALESCE(SUM(high_risk_count),0) FROM erm.risk_assessment WHERE tenant_id = :t) AS high_cells,
                  (SELECT COALESCE(SUM(medium_risk_count),0) FROM erm.risk_assessment WHERE tenant_id = :t) AS medium_cells,
                  (SELECT COALESCE(SUM(low_risk_count),0) FROM erm.risk_assessment WHERE tenant_id = :t) AS low_cells
                """, Map.of("t", tenantId));
        return new OverviewRow(
                toInt(m.get("assets")),
                toInt(m.get("threats")),
                toInt(m.get("vulnerabilities")),
                toInt(m.get("measures")),
                toInt(m.get("assessments")),
                toInt(m.get("assessments_draft")),
                toInt(m.get("assessments_progress")),
                toInt(m.get("assessments_done")),
                toInt(m.get("high_cells")),
                toInt(m.get("medium_cells")),
                toInt(m.get("low_cells"))
        );
    }

    public List<RecentAssessmentRow> recentAssessments(long tenantId, int limit) {
        return jdbc.query("""
                SELECT id, title, status, high_risk_count, medium_risk_count, low_risk_count, updated_at
                FROM erm.risk_assessment WHERE tenant_id = :t ORDER BY updated_at DESC LIMIT :lim
                """, Map.of("t", tenantId, "lim", limit),
                (rs, i) -> new RecentAssessmentRow(
                        rs.getLong("id"),
                        rs.getString("title"),
                        rs.getString("status"),
                        rs.getInt("high_risk_count"),
                        rs.getInt("medium_risk_count"),
                        rs.getInt("low_risk_count"),
                        rs.getTimestamp("updated_at").toInstant().toString()
                ));
    }

    private static int toInt(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    public record OverviewRow(int assets, int threats, int vulnerabilities, int measures,
                              int assessments, int assessmentsDraft, int assessmentsInProgress, int assessmentsCompleted,
                              int highRiskCells, int mediumRiskCells, int lowRiskCells) {}

    public record RecentAssessmentRow(long id, String title, String status, int highCount, int mediumCount,
                                       int lowCount, String updatedAt) {}
}
