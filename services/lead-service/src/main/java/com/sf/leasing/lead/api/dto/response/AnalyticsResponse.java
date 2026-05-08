package com.sf.leasing.lead.api.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Aggregated KPI analytics response for Phase 5 dashboards.
 * Covers all 11 KPIs defined in Section 8.5 of the Implementation Plan.
 */
public class AnalyticsResponse {

    public String reportName;
    public String periodFrom;
    public String periodTo;
    public String branchCode;         // null = all branches

    // 1. Lead Aging buckets: 0-7 / 8-15 / 16-30 / 30+ days
    public AgingBuckets leadAging;

    // 2. First-Contact TAT (avg hours from assignment to first interaction)
    public Double avgFirstContactTatHours;

    // 3. Attempts per lead (avg call/message/email counters)
    public Double avgAttemptsPerLead;

    // 4. Response rate (% leads with ≥ 1 positive lessee response)
    public Double responseRatePercent;

    // 5. Data completeness score (% optional fields completed)
    public Double dataCompletenessScorePercent;

    // 6. Lead-to-Prospect conversion rate
    public Double leadToProspectConversionPercent;
    public Long totalLeadsCreated;
    public Long totalLeadsPromoted;

    // 7. Prospect-to-Customer conversion rate
    public Double prospectToCustomerConversionPercent;
    public Long totalProspects;
    public Long totalCustomersCreated;

    // 8. Closure reason distribution
    public List<Entry> closureReasonDistribution;

    // 9. Exception cure rate
    public Double exceptionCureRatePercent;
    public Long totalExceptions;
    public Long exceptionsResolvedWithinSla;

    // 10. Quote lock rate
    public Double quoteLockRatePercent;
    public Long totalOpportunities;
    public Long opportunitiesWithLockedQuote;

    // 11. CAM approval rate
    public Double camApprovalRatePercent;
    public Long totalCamSubmitted;
    public Long totalCamApproved;

    public static class AgingBuckets {
        public long bucket0To7;
        public long bucket8To15;
        public long bucket16To30;
        public long bucket30Plus;
    }

    public static class Entry {
        public String label;
        public long count;
        public Double percent;

        public Entry(String label, long count, Double percent) {
            this.label   = label;
            this.count   = count;
            this.percent = percent;
        }
    }
}
