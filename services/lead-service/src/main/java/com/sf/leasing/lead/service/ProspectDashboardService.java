package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.response.DashboardResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3 dashboards: Prospect Pipeline, Exception Queue, Lead→Prospect Conversion.
 */
@Service
public class ProspectDashboardService {

    private static final Logger LOG = LoggerFactory.getLogger(ProspectDashboardService.class);

    @PersistenceContext
    EntityManager em;

    /** Prospect Pipeline: count by status, optionally filtered by branch. */
    public DashboardResponse prospectPipeline(String branchCode) {
        String sql = branchCode != null
            ? "SELECT status, COUNT(*) FROM prospects WHERE assigned_branch_code = ?1 GROUP BY status"
            : "SELECT status, COUNT(*) FROM prospects GROUP BY status";

        List<Object[]> rows = branchCode != null
            ? em.createNativeQuery(sql).setParameter(1, branchCode).getResultList()
            : em.createNativeQuery(sql).getResultList();

        List<DashboardResponse.Entry> entries = new ArrayList<>();
        for (Object[] row : rows) {
            entries.add(new DashboardResponse.Entry(
                (String) row[0],
                ((Number) row[1]).longValue()
            ));
        }
        return new DashboardResponse("PROSPECT_PIPELINE", entries);
    }

    /** Exception Queue: pending validation failures with SLA status. */
    public DashboardResponse exceptionQueueSummary(String branchCode) {
        String sql =
            "SELECT eq.status, COUNT(*) " +
            "FROM exception_queue eq " +
            "WHERE eq.source_channel = 'PROSPECT_VALIDATION' " +
            "GROUP BY eq.status";

        List<Object[]> rows = em.createNativeQuery(sql).getResultList();

        List<DashboardResponse.Entry> entries = new ArrayList<>();
        for (Object[] row : rows) {
            entries.add(new DashboardResponse.Entry(
                (String) row[0],
                ((Number) row[1]).longValue()
            ));
        }
        return new DashboardResponse("EXCEPTION_QUEUE_SUMMARY", entries);
    }

    /** Lead→Prospect conversion: total leads vs prospects promoted in the last 30 days. */
    public DashboardResponse conversionMetrics(String branchCode) {
        String sqlLeads = branchCode != null
            ? "SELECT COUNT(*) FROM leads WHERE created_at >= NOW() - INTERVAL '30 days' AND assigned_branch_code = ?1"
            : "SELECT COUNT(*) FROM leads WHERE created_at >= NOW() - INTERVAL '30 days'";

        String sqlProspects = branchCode != null
            ? "SELECT COUNT(*) FROM prospects WHERE created_at >= NOW() - INTERVAL '30 days' AND assigned_branch_code = ?1"
            : "SELECT COUNT(*) FROM prospects WHERE created_at >= NOW() - INTERVAL '30 days'";

        Long leadCount = branchCode != null
            ? ((Number) em.createNativeQuery(sqlLeads).setParameter(1, branchCode).getSingleResult()).longValue()
            : ((Number) em.createNativeQuery(sqlLeads).getSingleResult()).longValue();

        Long prospectCount = branchCode != null
            ? ((Number) em.createNativeQuery(sqlProspects).setParameter(1, branchCode).getSingleResult()).longValue()
            : ((Number) em.createNativeQuery(sqlProspects).getSingleResult()).longValue();

        List<DashboardResponse.Entry> entries = new ArrayList<>();
        entries.add(new DashboardResponse.Entry("LEADS_CREATED_30D",    leadCount));
        entries.add(new DashboardResponse.Entry("PROSPECTS_CREATED_30D", prospectCount));
        if (leadCount > 0) {
            long pct = (prospectCount * 100L) / leadCount;
            entries.add(new DashboardResponse.Entry("CONVERSION_PCT", pct));
        }
        return new DashboardResponse("LEAD_TO_PROSPECT_CONVERSION", entries);
    }

    /** KYC validation outcomes: success/failure/override breakdown. */
    public DashboardResponse kycOutcomes(String branchCode) {
        String sql = branchCode != null
            ? "SELECT kv.status, COUNT(*) FROM prospect_kyc_validations kv " +
              "JOIN prospects p ON p.id = kv.prospect_id " +
              "WHERE p.assigned_branch_code = ?1 " +
              "GROUP BY kv.status"
            : "SELECT status, COUNT(*) FROM prospect_kyc_validations GROUP BY status";

        List<Object[]> rows = branchCode != null
            ? em.createNativeQuery(sql).setParameter(1, branchCode).getResultList()
            : em.createNativeQuery(sql).getResultList();

        List<DashboardResponse.Entry> entries = new ArrayList<>();
        for (Object[] row : rows) {
            entries.add(new DashboardResponse.Entry(
                (String) row[0],
                ((Number) row[1]).longValue()
            ));
        }
        return new DashboardResponse("KYC_OUTCOMES", entries);
    }

    // -------------------------------------------------------
    // Phase 4 dashboard additions (PP5–PP7)
    // -------------------------------------------------------

    /** Opportunity pipeline: count by status across all prospects. */
    public DashboardResponse opportunityPipeline(String branchCode) {
        String sql = branchCode != null
            ? "SELECT o.status, COUNT(*) FROM opportunities o " +
              "JOIN prospects p ON p.id = o.prospect_uuid " +
              "WHERE p.assigned_branch_code = ?1 GROUP BY o.status"
            : "SELECT status, COUNT(*) FROM opportunities GROUP BY status";

        List<Object[]> rows = branchCode != null
            ? em.createNativeQuery(sql).setParameter(1, branchCode).getResultList()
            : em.createNativeQuery(sql).getResultList();

        List<DashboardResponse.Entry> entries = new ArrayList<>();
        for (Object[] row : rows) {
            entries.add(new DashboardResponse.Entry(
                (String) row[0], ((Number) row[1]).longValue()));
        }
        return new DashboardResponse("OPPORTUNITY_PIPELINE", entries);
    }

    /** Application status distribution: by status. */
    public DashboardResponse applicationStatusDistribution(String branchCode) {
        String sql = branchCode != null
            ? "SELECT a.status, COUNT(*) FROM applications a " +
              "JOIN prospects p ON p.id = a.prospect_uuid " +
              "WHERE p.assigned_branch_code = ?1 GROUP BY a.status"
            : "SELECT status, COUNT(*) FROM applications GROUP BY status";

        List<Object[]> rows = branchCode != null
            ? em.createNativeQuery(sql).setParameter(1, branchCode).getResultList()
            : em.createNativeQuery(sql).getResultList();

        List<DashboardResponse.Entry> entries = new ArrayList<>();
        for (Object[] row : rows) {
            entries.add(new DashboardResponse.Entry(
                (String) row[0], ((Number) row[1]).longValue()));
        }
        return new DashboardResponse("APPLICATION_STATUS", entries);
    }
}
