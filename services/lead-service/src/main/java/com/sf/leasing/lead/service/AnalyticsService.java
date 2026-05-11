package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.response.AnalyticsResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 5 full analytics platform — Section 8.5 of the Implementation Plan.
 *
 * Computes all 11 KPIs:
 *   1. Lead aging (4 buckets)
 *   2. First-contact TAT (avg hours)
 *   3. Attempts per lead (avg)
 *   4. Response rate (%)
 *   5. Data completeness score (%)
 *   6. Lead-to-Prospect conversion rate (%)
 *   7. Prospect-to-Customer conversion rate (%)
 *   8. Closure reason distribution
 *   9. Exception cure rate (%)
 *  10. Quote lock rate (%)
 *  11. CAM approval rate (%)
 */
@Service
public class AnalyticsService {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyticsService.class);

    @PersistenceContext
    EntityManager em;

    // -------------------------------------------------------
    // Full analytics report (all 11 KPIs in one call)
    // -------------------------------------------------------

    public AnalyticsResponse computeFullReport(String periodFrom, String periodTo,
                                                String branchCode) {
        LOG.info("Computing full analytics: period={} to {} branch={}", periodFrom, periodTo, branchCode);

        AnalyticsResponse report = new AnalyticsResponse();
        report.reportName  = "FULL_KPI_REPORT";
        report.periodFrom  = periodFrom;
        report.periodTo    = periodTo;
        report.branchCode  = branchCode;

        String branch = branchCode;

        report.leadAging                      = computeLeadAging(periodFrom, periodTo, branch);
        report.avgFirstContactTatHours        = computeFirstContactTat(periodFrom, periodTo, branch);
        report.avgAttemptsPerLead             = computeAvgAttempts(periodFrom, periodTo, branch);
        report.responseRatePercent            = computeResponseRate(periodFrom, periodTo, branch);
        report.dataCompletenessScorePercent   = computeDataCompleteness(periodFrom, periodTo, branch);

        long[] leadConversion                 = computeLeadToProspectConversion(periodFrom, periodTo, branch);
        report.totalLeadsCreated              = leadConversion[0];
        report.totalLeadsPromoted             = leadConversion[1];
        report.leadToProspectConversionPercent = safePercent(leadConversion[1], leadConversion[0]);

        long[] prospectConversion             = computeProspectToCustomerConversion(periodFrom, periodTo, branch);
        report.totalProspects                 = prospectConversion[0];
        report.totalCustomersCreated          = prospectConversion[1];
        report.prospectToCustomerConversionPercent = safePercent(prospectConversion[1], prospectConversion[0]);

        report.closureReasonDistribution      = computeClosureReasonDistribution(periodFrom, periodTo, branch);
        long[] exceptionCure                  = computeExceptionCureRate(periodFrom, periodTo);
        report.totalExceptions                = exceptionCure[0];
        report.exceptionsResolvedWithinSla    = exceptionCure[1];
        report.exceptionCureRatePercent       = safePercent(exceptionCure[1], exceptionCure[0]);

        long[] quoteLock                      = computeQuoteLockRate(periodFrom, periodTo, branch);
        report.totalOpportunities             = quoteLock[0];
        report.opportunitiesWithLockedQuote   = quoteLock[1];
        report.quoteLockRatePercent           = safePercent(quoteLock[1], quoteLock[0]);

        long[] camApproval                    = computeCamApprovalRate(periodFrom, periodTo, branch);
        report.totalCamSubmitted              = camApproval[0];
        report.totalCamApproved               = camApproval[1];
        report.camApprovalRatePercent         = safePercent(camApproval[1], camApproval[0]);

        return report;
    }

    // -------------------------------------------------------
    // KPI 1: Lead aging (0-7 / 8-15 / 16-30 / 30+ days)
    // -------------------------------------------------------

    public AnalyticsResponse.AgingBuckets computeLeadAging(String periodFrom, String periodTo,
                                                            String branchCode) {
        String periodFilter = buildPeriodFilter("l.created_at", periodFrom, periodTo);
        String branchFilter = branchCode != null
            ? " AND l.assigned_branch_code = '" + branchCode + "'" : "";

        String sql =
            "SELECT " +
            "  SUM(CASE WHEN age_days BETWEEN 0  AND  7 THEN 1 ELSE 0 END) AS b0_7,  " +
            "  SUM(CASE WHEN age_days BETWEEN 8  AND 15 THEN 1 ELSE 0 END) AS b8_15, " +
            "  SUM(CASE WHEN age_days BETWEEN 16 AND 30 THEN 1 ELSE 0 END) AS b16_30," +
            "  SUM(CASE WHEN age_days > 30              THEN 1 ELSE 0 END) AS b30p   " +
            "FROM (SELECT EXTRACT(DAY FROM (CURRENT_TIMESTAMP - l.created_at)) AS age_days " +
            "      FROM leads l WHERE l.status NOT IN ('CLOSED','PROMOTED')" +
            periodFilter + branchFilter + ") sub";

        Object[] row = (Object[]) em.createNativeQuery(sql).getSingleResult();
        AnalyticsResponse.AgingBuckets buckets = new AnalyticsResponse.AgingBuckets();
        buckets.bucket0To7   = row[0] != null ? ((Number) row[0]).longValue() : 0L;
        buckets.bucket8To15  = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        buckets.bucket16To30 = row[2] != null ? ((Number) row[2]).longValue() : 0L;
        buckets.bucket30Plus = row[3] != null ? ((Number) row[3]).longValue() : 0L;
        return buckets;
    }

    // -------------------------------------------------------
    // KPI 2: First-contact TAT (avg hours from assignment to first interaction)
    // -------------------------------------------------------

    public Double computeFirstContactTat(String periodFrom, String periodTo, String branchCode) {
        String branchFilter = branchCode != null
            ? " AND l.assigned_branch_code = '" + branchCode + "'" : "";

        String sql =
            "SELECT AVG(EXTRACT(EPOCH FROM (i.created_at - la.assigned_at)) / 3600) " +
            "FROM leads l " +
            "JOIN lead_assignments la ON la.lead_id = l.id AND la.hierarchy_level = 'FIELD_OFFICER' " +
            "JOIN interactions i ON i.lead_id = l.id " +
            "WHERE i.created_at = (SELECT MIN(i2.created_at) FROM interactions i2 WHERE i2.lead_id = l.id)" +
            buildPeriodFilter("l.created_at", periodFrom, periodTo) + branchFilter;

        Object result = em.createNativeQuery(sql).getSingleResult();
        return result != null ? ((Number) result).doubleValue() : null;
    }

    // -------------------------------------------------------
    // KPI 3: Attempts per lead (avg)
    // -------------------------------------------------------

    public Double computeAvgAttempts(String periodFrom, String periodTo, String branchCode) {
        String branchFilter = branchCode != null
            ? " AND l.assigned_branch_code = '" + branchCode + "'" : "";
        String periodFilter = buildPeriodFilter("l.created_at", periodFrom, periodTo);

        String sql =
            "SELECT AVG(call_attempts + message_attempts + email_attempts) " +
            "FROM leads l WHERE 1=1" + periodFilter + branchFilter;

        Object result = em.createNativeQuery(sql).getSingleResult();
        return result != null ? ((Number) result).doubleValue() : 0.0;
    }

    // -------------------------------------------------------
    // KPI 4: Response rate (% leads with >= 1 positive lessee response)
    // -------------------------------------------------------

    public Double computeResponseRate(String periodFrom, String periodTo, String branchCode) {
        String branchFilter = branchCode != null
            ? " AND l.assigned_branch_code = '" + branchCode + "'" : "";
        String periodFilter = buildPeriodFilter("l.created_at", periodFrom, periodTo);

        String totalSql = "SELECT COUNT(*) FROM leads l WHERE 1=1" + periodFilter + branchFilter;
        String respondedSql =
            "SELECT COUNT(DISTINCT l.id) FROM leads l " +
            "JOIN interactions i ON i.lead_id = l.id AND i.outcome = 'POSITIVE' WHERE 1=1" +
            periodFilter + branchFilter;

        long total    = ((Number) em.createNativeQuery(totalSql).getSingleResult()).longValue();
        long responded = ((Number) em.createNativeQuery(respondedSql).getSingleResult()).longValue();
        return safePercent(responded, total);
    }

    // -------------------------------------------------------
    // KPI 5: Data completeness score
    // -------------------------------------------------------

    public Double computeDataCompleteness(String periodFrom, String periodTo, String branchCode) {
        String branchFilter = branchCode != null
            ? " AND assigned_branch_code = '" + branchCode + "'" : "";
        String periodFilter = buildPeriodFilter("created_at", periodFrom, periodTo);

        // Optional fields: email, mobile, pan, gstin, address
        String sql =
            "SELECT AVG(score) FROM (" +
            "  SELECT (" +
            "    CASE WHEN pan IS NOT NULL THEN 1 ELSE 0 END + " +
            "    CASE WHEN gstin IS NOT NULL THEN 1 ELSE 0 END + " +
            "    CASE WHEN source_name IS NOT NULL THEN 1 ELSE 0 END" +
            "  ) * 100.0 / 3 AS score " +
            "  FROM leads WHERE 1=1" + periodFilter + branchFilter +
            ") sub";

        Object result = em.createNativeQuery(sql).getSingleResult();
        return result != null ? ((Number) result).doubleValue() : 0.0;
    }

    // -------------------------------------------------------
    // KPI 6: Lead-to-Prospect conversion rate
    // -------------------------------------------------------

    public long[] computeLeadToProspectConversion(String periodFrom, String periodTo, String branchCode) {
        String branchFilter = branchCode != null
            ? " AND assigned_branch_code = '" + branchCode + "'" : "";
        String periodFilter = buildPeriodFilter("created_at", periodFrom, periodTo);

        String totalSql   = "SELECT COUNT(*) FROM leads WHERE 1=1" + periodFilter + branchFilter;
        String promotedSql = "SELECT COUNT(*) FROM leads WHERE status = 'PROMOTED'" + periodFilter + branchFilter;

        long total   = ((Number) em.createNativeQuery(totalSql).getSingleResult()).longValue();
        long promoted = ((Number) em.createNativeQuery(promotedSql).getSingleResult()).longValue();
        return new long[]{total, promoted};
    }

    // -------------------------------------------------------
    // KPI 7: Prospect-to-Customer conversion rate
    // -------------------------------------------------------

    public long[] computeProspectToCustomerConversion(String periodFrom, String periodTo, String branchCode) {
        String periodFilter = buildPeriodFilter("created_at", periodFrom, periodTo);

        String prospectSql  = "SELECT COUNT(*) FROM prospects WHERE 1=1" + periodFilter;
        String customerSql  = "SELECT COUNT(*) FROM customers WHERE 1=1" + periodFilter;

        long prospects = ((Number) em.createNativeQuery(prospectSql).getSingleResult()).longValue();
        long customers = ((Number) em.createNativeQuery(customerSql).getSingleResult()).longValue();
        return new long[]{prospects, customers};
    }

    // -------------------------------------------------------
    // KPI 8: Closure reason distribution
    // -------------------------------------------------------

    @SuppressWarnings("unchecked")
    public List<AnalyticsResponse.Entry> computeClosureReasonDistribution(String periodFrom,
                                                                           String periodTo,
                                                                           String branchCode) {
        String branchFilter = branchCode != null
            ? " AND assigned_branch_code = '" + branchCode + "'" : "";
        String periodFilter = buildPeriodFilter("closed_at", periodFrom, periodTo);

        String countSql = "SELECT COUNT(*) FROM leads WHERE status='CLOSED'" + periodFilter + branchFilter;
        long total = ((Number) em.createNativeQuery(countSql).getSingleResult()).longValue();

        String sql =
            "SELECT closure_reason_code, COUNT(*) AS cnt " +
            "FROM leads WHERE status = 'CLOSED'" + periodFilter + branchFilter +
            " GROUP BY closure_reason_code ORDER BY cnt DESC";

        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        List<AnalyticsResponse.Entry> entries = new ArrayList<>();
        for (Object[] row : rows) {
            String label = row[0] != null ? row[0].toString() : "UNKNOWN";
            long count   = ((Number) row[1]).longValue();
            entries.add(new AnalyticsResponse.Entry(label, count, safePercent(count, total)));
        }
        return entries;
    }

    // -------------------------------------------------------
    // KPI 9: Exception cure rate
    // -------------------------------------------------------

    public long[] computeExceptionCureRate(String periodFrom, String periodTo) {
        String periodFilter = buildPeriodFilter("created_at", periodFrom, periodTo);

        String totalSql   = "SELECT COUNT(*) FROM exception_queue_records WHERE 1=1" + periodFilter;
        String resolvedSql =
            "SELECT COUNT(*) FROM exception_queue_records " +
            "WHERE resolved_at IS NOT NULL AND resolved_at <= cure_sla_deadline" + periodFilter;

        long total   = ((Number) em.createNativeQuery(totalSql).getSingleResult()).longValue();
        long resolved = ((Number) em.createNativeQuery(resolvedSql).getSingleResult()).longValue();
        return new long[]{total, resolved};
    }

    // -------------------------------------------------------
    // KPI 10: Quote lock rate
    // -------------------------------------------------------

    public long[] computeQuoteLockRate(String periodFrom, String periodTo, String branchCode) {
        String periodFilter = buildPeriodFilter("o.created_at", periodFrom, periodTo);

        String totalSql =
            "SELECT COUNT(*) FROM opportunities o WHERE 1=1" + periodFilter;
        String lockedSql =
            "SELECT COUNT(DISTINCT q.opportunity_id) FROM quotes q " +
            "JOIN opportunities o ON o.id = q.opportunity_id " +
            "WHERE q.status = 'LOCKED'" + periodFilter;

        long total  = ((Number) em.createNativeQuery(totalSql).getSingleResult()).longValue();
        long locked = ((Number) em.createNativeQuery(lockedSql).getSingleResult()).longValue();
        return new long[]{total, locked};
    }

    // -------------------------------------------------------
    // KPI 11: CAM approval rate
    // -------------------------------------------------------

    public long[] computeCamApprovalRate(String periodFrom, String periodTo, String branchCode) {
        String periodFilter = buildPeriodFilter("initiated_at", periodFrom, periodTo);

        String submittedSql =
            "SELECT COUNT(*) FROM cam_workflows WHERE cam_status != 'NOT_STARTED'" + periodFilter;
        String approvedSql  =
            "SELECT COUNT(*) FROM cam_workflows WHERE cam_status = 'APPROVED'" + periodFilter;

        long submitted = ((Number) em.createNativeQuery(submittedSql).getSingleResult()).longValue();
        long approved  = ((Number) em.createNativeQuery(approvedSql).getSingleResult()).longValue();
        return new long[]{submitted, approved};
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private String buildPeriodFilter(String column, String from, String to) {
        StringBuilder sb = new StringBuilder();
        if (from != null && !from.isBlank()) {
            sb.append(" AND ").append(column).append(" >= '").append(from).append("'");
        }
        if (to != null && !to.isBlank()) {
            sb.append(" AND ").append(column).append(" <= '").append(to).append("'");
        }
        return sb.toString();
    }

    private Double safePercent(long numerator, long denominator) {
        if (denominator == 0L) return 0.0;
        return (numerator * 100.0) / denominator;
    }
}
