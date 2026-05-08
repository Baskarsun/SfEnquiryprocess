package com.sf.leasing.lead.infrastructure.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

@ApplicationScoped
public class LeadEventProducer {

    private static final Logger LOG = Logger.getLogger(LeadEventProducer.class);

    @Inject
    @Channel("lead-events")
    Emitter<String> emitter;

    public void publishLeadCreated(String lrn, String tempCustomerNumber, String createdBy, String channel) {
        String payload = buildEvent("LeadCreated", Map.of(
            "lrn", lrn,
            "tempCustomerNumber", tempCustomerNumber,
            "createdBy", createdBy,
            "channel", channel,
            "timestamp", Instant.now().toString()
        ));
        send(payload);
        LOG.infof("Event LeadCreated published: LRN=%s", lrn);
    }

    public void publishLeadAssigned(String lrn, String assignedTo, String level) {
        String payload = buildEvent("LeadAssigned", Map.of(
            "lrn", lrn,
            "assignedTo", assignedTo,
            "hierarchyLevel", level,
            "timestamp", Instant.now().toString()
        ));
        send(payload);
        LOG.infof("Event LeadAssigned published: LRN=%s -> %s", lrn, assignedTo);
    }

    public void publishLeadPromoted(String lrn, String prospectUuid, String promotedBy) {
        String payload = buildEvent("LeadPromoted", Map.of(
            "lrn", lrn,
            "prospectUuid", prospectUuid,
            "promotedBy", promotedBy,
            "timestamp", Instant.now().toString()
        ));
        send(payload);
        LOG.infof("Event LeadPromoted published: LRN=%s prospectUuid=%s", lrn, prospectUuid);
    }

    // -------------------------------------------------------
    // Phase 4 events
    // -------------------------------------------------------

    public void publishOpportunityCreated(String opportunityId, String prospectId, String lobTag, String createdBy) {
        String payload = buildEvent("OpportunityCreated", Map.of(
            "opportunityId", opportunityId,
            "prospectId",    safeStr(prospectId),
            "lobTag",        lobTag,
            "createdBy",     createdBy,
            "timestamp",     Instant.now().toString()
        ));
        send(payload);
        LOG.infof("Event OpportunityCreated published: OPP=%s", opportunityId);
    }

    public void publishQuoteLocked(String quoteId, String opportunityId, String lockedBy) {
        String payload = buildEvent("QuoteLocked", Map.of(
            "quoteId",       quoteId,
            "opportunityId", safeStr(opportunityId),
            "lockedBy",      lockedBy,
            "timestamp",     Instant.now().toString()
        ));
        send(payload);
        LOG.infof("Event QuoteLocked published: QT=%s OPP=%s", quoteId, opportunityId);
    }

    public void publishApplicationInitiated(String applicationId, String prospectId,
                                             String opportunityId, String initiatedBy) {
        String payload = buildEvent("ApplicationInitiated", Map.of(
            "applicationId", applicationId,
            "prospectId",    safeStr(prospectId),
            "opportunityId", safeStr(opportunityId),
            "initiatedBy",   initiatedBy,
            "timestamp",     Instant.now().toString()
        ));
        send(payload);
        LOG.infof("Event ApplicationInitiated published: APP=%s", applicationId);
    }

    public void publishApplicationEligible(String applicationId, String prospectId) {
        String payload = buildEvent("ApplicationEligible", Map.of(
            "applicationId", applicationId,
            "prospectId",    safeStr(prospectId),
            "timestamp",     Instant.now().toString()
        ));
        send(payload);
        LOG.infof("Event ApplicationEligible published: APP=%s", applicationId);
    }

    public void publishCamApproved(String applicationId, String prospectId,
                                    String sanctionId, String approvedBy) {
        String payload = buildEvent("CamApproved", Map.of(
            "applicationId", applicationId,
            "prospectId",    safeStr(prospectId),
            "sanctionId",    safeStr(sanctionId),
            "approvedBy",    approvedBy,
            "timestamp",     Instant.now().toString()
        ));
        send(payload);
        LOG.infof("Event CamApproved published: APP=%s sanctionId=%s", applicationId, sanctionId);
    }

    // -------------------------------------------------------
    // Phase 5 events
    // -------------------------------------------------------

    public void publishCustomerCreated(String customerId, String prospectId,
                                        String applicationId, String createdBy) {
        String payload = buildEvent("CustomerCreated", Map.of(
            "customerId",    customerId,
            "prospectId",    safeStr(prospectId),
            "applicationId", safeStr(applicationId),
            "createdBy",     createdBy,
            "timestamp",     Instant.now().toString()
        ));
        send(payload);
        LOG.infof("Event CustomerCreated published: CUST=%s PROSPECT=%s", customerId, prospectId);
    }

    public void publishOpportunityWon(String opportunityId, String prospectId, String customerId) {
        String payload = buildEvent("OpportunityWon", Map.of(
            "opportunityId", opportunityId,
            "prospectId",    safeStr(prospectId),
            "customerId",    safeStr(customerId),
            "timestamp",     Instant.now().toString()
        ));
        send(payload);
        LOG.infof("Event OpportunityWon published: OPP=%s CUST=%s", opportunityId, customerId);
    }

    public void publishLmsUpdated(String lrn, String prospectId, String customerId) {
        String payload = buildEvent("LmsUpdated", Map.of(
            "lrn",        safeStr(lrn),
            "prospectId", safeStr(prospectId),
            "customerId", safeStr(customerId),
            "timestamp",  Instant.now().toString()
        ));
        send(payload);
        LOG.infof("Event LmsUpdated published: LRN=%s CUST=%s", lrn, customerId);
    }

    private static String safeStr(String value) {
        return value != null ? value : "";
    }

    private void send(String payload) {
        try {
            emitter.send(payload);
        } catch (Exception e) {
            // Event publishing failure must not block the business transaction
            LOG.warnf("Failed to publish event (suppressed): %s", e.getMessage());
        }
    }

    private String buildEvent(String type, Map<String, String> data) {
        StringBuilder sb = new StringBuilder("{\"eventType\":\"").append(type).append("\"");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            sb.append(",\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }
}
