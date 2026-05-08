package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.OverrideTemperatureRequest;
import com.sf.leasing.lead.domain.enums.LeadStatus;
import com.sf.leasing.lead.domain.enums.LeadTemperature;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.domain.model.TemperatureAudit;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implements LP7: Lead Temperature Engine.
 *
 * LP7.1 — Temperature classification (Hot / Warm / Cold).
 * LP7.2 — Scheduled daily degradation with configurable thresholds.
 * LP7.3 — Manual override with mandatory reason code + audit trail.
 * LP7.4 — Closure suggestion when N unanswered attempts accumulate.
 */
@ApplicationScoped
public class TemperatureEngineService {

    private static final Logger LOG = Logger.getLogger(TemperatureEngineService.class);

    @ConfigProperty(name = "temperature.hot-warn-days", defaultValue = "7")
    int hotWarnDays;

    @ConfigProperty(name = "temperature.warm-cold-days", defaultValue = "14")
    int warmColdDays;

    @ConfigProperty(name = "temperature.unanswered-closure-threshold", defaultValue = "5")
    int closureThreshold;

    @Inject
    EntityManager em;

    // -------------------------------------------------------
    // LP7.3: Manual temperature override
    // -------------------------------------------------------

    @Transactional
    public void overrideTemperature(String lrn, OverrideTemperatureRequest req, String userId) {
        Lead lead = Lead.findByLrn(lrn);
        if (lead == null) throw new BusinessException("LEAD_NOT_FOUND", "Lead not found: " + lrn);
        if (lead.isClosed()) throw new BusinessException("LEAD_CLOSED", "Cannot override temperature on a closed lead.");

        LeadTemperature previous = lead.temperature;
        writeAuditEntry(lead, previous, req.temperature, req.reasonCode, req.reasonText, "MANUAL", userId);

        lead.temperature             = req.temperature;
        lead.temperatureReasonCode   = req.reasonCode;
        lead.temperatureReasonText   = req.reasonText;
        lead.temperatureSuggestedBy  = "MANUAL";
        lead.updatedBy               = userId;
        lead.updatedAt               = LocalDateTime.now();

        LOG.infof("Temperature manually overridden: LRN=%s %s→%s by %s reason=%s",
            lrn, previous, req.temperature, userId, req.reasonCode);
    }

    // -------------------------------------------------------
    // LP7.1: Compute temperature for a single lead
    // -------------------------------------------------------

    public LeadTemperature computeTemperature(Lead lead) {
        if (lead.isClosed() || lead.isPromoted()) return lead.temperature;

        LocalDateTime lastActivity = (em != null && lead.id != null) ? getLastActivityTime(lead) : null;
        long daysSinceActivity = lastActivity == null
            ? daysSince(lead.createdAt)
            : daysSince(lastActivity);

        if (daysSinceActivity <= 2 || lead.callAttempts + lead.messageAttempts + lead.emailAttempts >= 3) {
            return LeadTemperature.HOT;
        }
        if (daysSinceActivity <= hotWarnDays) {
            return LeadTemperature.WARM;
        }
        return LeadTemperature.COLD;
    }

    // -------------------------------------------------------
    // LP7.2: Scheduled daily degradation
    // -------------------------------------------------------

    @Transactional
    public void runDailyDegradation() {
        degradeHotToWarm();
        degradeWarmToCold();
        suggestClosureForStaleLeads();
    }

    private void degradeHotToWarm() {
        List<Object[]> hotLeads = em.createNativeQuery(
            "SELECT l.id, l.lrn FROM leads l " +
            "WHERE l.temperature = 'HOT' AND l.status NOT IN ('CLOSED', 'PROMOTED') " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM interactions i WHERE i.lead_id = l.id " +
            "  AND i.interaction_timestamp > (CURRENT_TIMESTAMP - INTERVAL '" + hotWarnDays + " days')" +
            ")"
        ).getResultList();

        for (Object[] row : hotLeads) {
            degradeSingleLead(row[0], row[1], LeadTemperature.HOT, LeadTemperature.WARM,
                "TEMP_DEGRADATION", "Hot→Warm: no activity for " + hotWarnDays + " days");
        }
        if (!hotLeads.isEmpty()) {
            LOG.infof("Temperature degradation: %d leads degraded HOT→WARM", hotLeads.size());
        }
    }

    private void degradeWarmToCold() {
        List<Object[]> warmLeads = em.createNativeQuery(
            "SELECT l.id, l.lrn FROM leads l " +
            "WHERE l.temperature = 'WARM' AND l.status NOT IN ('CLOSED', 'PROMOTED') " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM interactions i WHERE i.lead_id = l.id " +
            "  AND i.interaction_timestamp > (CURRENT_TIMESTAMP - INTERVAL '" + warmColdDays + " days')" +
            ")"
        ).getResultList();

        for (Object[] row : warmLeads) {
            degradeSingleLead(row[0], row[1], LeadTemperature.WARM, LeadTemperature.COLD,
                "TEMP_DEGRADATION", "Warm→Cold: no activity for " + warmColdDays + " days");
        }
        if (!warmLeads.isEmpty()) {
            LOG.infof("Temperature degradation: %d leads degraded WARM→COLD", warmLeads.size());
        }
    }

    /**
     * LP7.4: Closure suggestion — N unanswered attempts with negative outcome.
     */
    private void suggestClosureForStaleLeads() {
        List<Object[]> staleLeads = em.createNativeQuery(
            "SELECT l.id, l.lrn, (l.call_attempts + l.message_attempts + l.email_attempts) AS attempts " +
            "FROM leads l " +
            "WHERE l.status NOT IN ('CLOSED', 'PROMOTED') " +
            "AND (l.call_attempts + l.message_attempts + l.email_attempts) >= ?1 " +
            "AND NOT EXISTS (" +
            "  SELECT 1 FROM interactions i WHERE i.lead_id = l.id AND i.outcome_notes ILIKE '%positive%'" +
            ")"
        ).setParameter(1, closureThreshold).getResultList();

        for (Object[] row : staleLeads) {
            LOG.infof("LP7.4 closure suggestion: LRN=%s attempts=%s", row[1], row[2]);
        }
        if (!staleLeads.isEmpty()) {
            LOG.infof("Closure suggestion flagged for %d stale leads (threshold=%d attempts)",
                staleLeads.size(), closureThreshold);
        }
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    private void degradeSingleLead(Object leadIdObj, Object lrnObj,
                                    LeadTemperature from, LeadTemperature to,
                                    String reasonCode, String reasonText) {
        em.createNativeQuery(
            "UPDATE leads SET temperature = ?1, temperature_suggested_by = 'SYSTEM', " +
            "temperature_reason_code = ?2, temperature_reason_text = ?3, updated_at = CURRENT_TIMESTAMP " +
            "WHERE id = ?4::uuid"
        )
            .setParameter(1, to.name())
            .setParameter(2, reasonCode)
            .setParameter(3, reasonText)
            .setParameter(4, leadIdObj.toString())
            .executeUpdate();

        // Append-only audit entry
        em.createNativeQuery(
            "INSERT INTO temperature_audit (lead_id, previous_temperature, new_temperature, " +
            "reason_code, reason_text, suggested_by, changed_at) " +
            "VALUES (?1::uuid, ?2, ?3, ?4, ?5, 'SYSTEM', CURRENT_TIMESTAMP)"
        )
            .setParameter(1, leadIdObj.toString())
            .setParameter(2, from.name())
            .setParameter(3, to.name())
            .setParameter(4, reasonCode)
            .setParameter(5, reasonText)
            .executeUpdate();
    }

    private void writeAuditEntry(Lead lead, LeadTemperature from, LeadTemperature to,
                                  String reasonCode, String reasonText,
                                  String suggestedBy, String changedBy) {
        TemperatureAudit audit = new TemperatureAudit();
        audit.lead                = lead;
        audit.previousTemperature = from != null ? from.name() : null;
        audit.newTemperature      = to.name();
        audit.reasonCode          = reasonCode;
        audit.reasonText          = reasonText;
        audit.suggestedBy         = suggestedBy;
        audit.changedBy           = changedBy;
        audit.changedAt           = LocalDateTime.now();
        audit.persist();
    }

    private LocalDateTime getLastActivityTime(Lead lead) {
        List<Object[]> rows = em.createNativeQuery(
            "SELECT MAX(interaction_timestamp) FROM interactions WHERE lead_id = ?1"
        ).setParameter(1, lead.id).getResultList();
        if (rows.isEmpty() || rows.get(0)[0] == null) return null;
        return ((java.sql.Timestamp) rows.get(0)[0]).toLocalDateTime();
    }

    private long daysSince(LocalDateTime dateTime) {
        return java.time.temporal.ChronoUnit.DAYS.between(dateTime, LocalDateTime.now());
    }
}
