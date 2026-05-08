package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.response.DashboardResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 2 dashboard queries — enhanced dashboards per Section 5.7 of the implementation plan.
 *
 * Reports:
 *   1. Temperature distribution (Hot / Warm / Cold counts)
 *   2. SLA compliance tracker (compliant vs. breached)
 *   3. Aging report (0-7 / 8-15 / 16-30 / 30+ days)
 *   4. Marketing source report (leads by Source Category + Source Name)
 */
@ApplicationScoped
public class DashboardService {

    private static final Logger LOG = Logger.getLogger(DashboardService.class);

    @Inject
    EntityManager em;

    // -------------------------------------------------------
    // Temperature distribution
    // -------------------------------------------------------

    public DashboardResponse temperatureDistribution(String branchCode) {
        String sql = "SELECT temperature, COUNT(*) FROM leads WHERE status NOT IN ('CLOSED', 'PROMOTED')" +
            (branchCode != null ? " AND assigned_branch_code = '" + branchCode + "'" : "") +
            " GROUP BY temperature ORDER BY temperature";

        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        List<DashboardResponse.Entry> entries = new ArrayList<>();
        for (Object[] row : rows) {
            entries.add(new DashboardResponse.Entry(
                row[0] != null ? row[0].toString() : "UNKNOWN",
                ((Number) row[1]).longValue()
            ));
        }
        LOG.debugf("Temperature distribution: %d buckets", entries.size());
        return new DashboardResponse("TEMPERATURE_DISTRIBUTION", entries);
    }

    // -------------------------------------------------------
    // SLA compliance tracker
    // -------------------------------------------------------

    public DashboardResponse slaComplianceTracker(String branchCode) {
        String branchFilter = branchCode != null
            ? " AND l.assigned_branch_code = '" + branchCode + "'" : "";

        String sql =
            "SELECT " +
            "  SUM(CASE WHEN la.sla_breached = FALSE THEN 1 ELSE 0 END) AS compliant, " +
            "  SUM(CASE WHEN la.sla_breached = TRUE  THEN 1 ELSE 0 END) AS breached " +
            "FROM lead_assignments la " +
            "JOIN leads l ON l.id = la.lead_id " +
            "WHERE l.status NOT IN ('CLOSED') " + branchFilter;

        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        List<DashboardResponse.Entry> entries = new ArrayList<>();
        if (!rows.isEmpty() && rows.get(0)[0] != null) {
            entries.add(new DashboardResponse.Entry("Compliant", ((Number) rows.get(0)[0]).longValue()));
            entries.add(new DashboardResponse.Entry("Breached",  ((Number) rows.get(0)[1]).longValue()));
        }
        return new DashboardResponse("SLA_COMPLIANCE", entries);
    }

    // -------------------------------------------------------
    // Aging report
    // -------------------------------------------------------

    public DashboardResponse agingReport(String branchCode) {
        String branchFilter = branchCode != null
            ? " AND assigned_branch_code = '" + branchCode + "'" : "";

        String sql =
            "SELECT " +
            "  SUM(CASE WHEN EXTRACT(DAY FROM NOW() - created_at) BETWEEN 0  AND 7  THEN 1 ELSE 0 END) AS \"0-7\", " +
            "  SUM(CASE WHEN EXTRACT(DAY FROM NOW() - created_at) BETWEEN 8  AND 15 THEN 1 ELSE 0 END) AS \"8-15\", " +
            "  SUM(CASE WHEN EXTRACT(DAY FROM NOW() - created_at) BETWEEN 16 AND 30 THEN 1 ELSE 0 END) AS \"16-30\", " +
            "  SUM(CASE WHEN EXTRACT(DAY FROM NOW() - created_at) > 30              THEN 1 ELSE 0 END) AS \"30+\" " +
            "FROM leads WHERE status NOT IN ('CLOSED', 'PROMOTED') " + branchFilter;

        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        List<DashboardResponse.Entry> entries = new ArrayList<>();
        if (!rows.isEmpty() && rows.get(0)[0] != null) {
            entries.add(new DashboardResponse.Entry("0-7 days",  ((Number) rows.get(0)[0]).longValue()));
            entries.add(new DashboardResponse.Entry("8-15 days", ((Number) rows.get(0)[1]).longValue()));
            entries.add(new DashboardResponse.Entry("16-30 days",((Number) rows.get(0)[2]).longValue()));
            entries.add(new DashboardResponse.Entry("30+ days",  ((Number) rows.get(0)[3]).longValue()));
        }
        return new DashboardResponse("AGING_REPORT", entries);
    }

    // -------------------------------------------------------
    // Marketing source report
    // -------------------------------------------------------

    public DashboardResponse marketingSourceReport(String branchCode) {
        String branchFilter = branchCode != null
            ? " AND assigned_branch_code = '" + branchCode + "'" : "";

        String sql =
            "SELECT source_category, source_name, COUNT(*) " +
            "FROM leads WHERE 1=1 " + branchFilter +
            " GROUP BY source_category, source_name " +
            "ORDER BY COUNT(*) DESC LIMIT 50";

        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        List<DashboardResponse.Entry> entries = new ArrayList<>();
        for (Object[] row : rows) {
            String label = row[0] + " / " + row[1];
            entries.add(new DashboardResponse.Entry(label, ((Number) row[2]).longValue()));
        }
        return new DashboardResponse("MARKETING_SOURCE_REPORT", entries);
    }
}
