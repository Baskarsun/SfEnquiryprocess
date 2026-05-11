package com.sf.leasing.lead.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class LeadEventProducer {

    private static final Logger LOG = LoggerFactory.getLogger(LeadEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topics.lead-events}")
    private String leadEventsTopic;

    @Value("${kafka.topics.customer-events}")
    private String customerEventsTopic;

    @Value("${kafka.topics.opportunity-events}")
    private String opportunityEventsTopic;

    @Value("${kafka.topics.lms-updates}")
    private String lmsUpdatesTopic;

    public LeadEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishLeadCreated(String lrn, String tempCustomerNumber, String createdBy, String channel) {
        String payload = buildEvent("LeadCreated", Map.of(
            "lrn", lrn,
            "tempCustomerNumber", tempCustomerNumber,
            "createdBy", createdBy,
            "channel", channel,
            "timestamp", Instant.now().toString()
        ));
        send(leadEventsTopic, lrn, payload);
        LOG.info("Event LeadCreated published: LRN={}", lrn);
    }

    public void publishLeadAssigned(String lrn, String assignedTo, String level) {
        String payload = buildEvent("LeadAssigned", Map.of(
            "lrn", lrn,
            "assignedTo", assignedTo,
            "hierarchyLevel", level,
            "timestamp", Instant.now().toString()
        ));
        send(leadEventsTopic, lrn, payload);
        LOG.info("Event LeadAssigned published: LRN={} -> {}", lrn, assignedTo);
    }

    public void publishLeadPromoted(String lrn, String prospectUuid, String promotedBy) {
        String payload = buildEvent("LeadPromoted", Map.of(
            "lrn", lrn,
            "prospectUuid", prospectUuid,
            "promotedBy", promotedBy,
            "timestamp", Instant.now().toString()
        ));
        send(leadEventsTopic, lrn, payload);
        LOG.info("Event LeadPromoted published: LRN={} prospectUuid={}", lrn, prospectUuid);
    }

    public void publishOpportunityCreated(String opportunityId, String prospectId, String lobTag, String createdBy) {
        String payload = buildEvent("OpportunityCreated", Map.of(
            "opportunityId", opportunityId,
            "prospectId",    safeStr(prospectId),
            "lobTag",        lobTag,
            "createdBy",     createdBy,
            "timestamp",     Instant.now().toString()
        ));
        send(opportunityEventsTopic, opportunityId, payload);
        LOG.info("Event OpportunityCreated published: OPP={}", opportunityId);
    }

    public void publishQuoteLocked(String quoteId, String opportunityId, String lockedBy) {
        String payload = buildEvent("QuoteLocked", Map.of(
            "quoteId",       quoteId,
            "opportunityId", safeStr(opportunityId),
            "lockedBy",      lockedBy,
            "timestamp",     Instant.now().toString()
        ));
        send(opportunityEventsTopic, quoteId, payload);
        LOG.info("Event QuoteLocked published: QT={} OPP={}", quoteId, opportunityId);
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
        send(leadEventsTopic, applicationId, payload);
        LOG.info("Event ApplicationInitiated published: APP={}", applicationId);
    }

    public void publishApplicationEligible(String applicationId, String prospectId) {
        String payload = buildEvent("ApplicationEligible", Map.of(
            "applicationId", applicationId,
            "prospectId",    safeStr(prospectId),
            "timestamp",     Instant.now().toString()
        ));
        send(leadEventsTopic, applicationId, payload);
        LOG.info("Event ApplicationEligible published: APP={}", applicationId);
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
        send(leadEventsTopic, applicationId, payload);
        LOG.info("Event CamApproved published: APP={} sanctionId={}", applicationId, sanctionId);
    }

    public void publishCustomerCreated(String customerId, String prospectId,
                                        String applicationId, String createdBy) {
        String payload = buildEvent("CustomerCreated", Map.of(
            "customerId",    customerId,
            "prospectId",    safeStr(prospectId),
            "applicationId", safeStr(applicationId),
            "createdBy",     createdBy,
            "timestamp",     Instant.now().toString()
        ));
        send(customerEventsTopic, customerId, payload);
        LOG.info("Event CustomerCreated published: CUST={} PROSPECT={}", customerId, prospectId);
    }

    public void publishOpportunityWon(String opportunityId, String prospectId, String customerId) {
        String payload = buildEvent("OpportunityWon", Map.of(
            "opportunityId", opportunityId,
            "prospectId",    safeStr(prospectId),
            "customerId",    safeStr(customerId),
            "timestamp",     Instant.now().toString()
        ));
        send(opportunityEventsTopic, opportunityId, payload);
        LOG.info("Event OpportunityWon published: OPP={} CUST={}", opportunityId, customerId);
    }

    public void publishLmsUpdated(String lrn, String prospectId, String customerId) {
        String payload = buildEvent("LmsUpdated", Map.of(
            "lrn",        safeStr(lrn),
            "prospectId", safeStr(prospectId),
            "customerId", safeStr(customerId),
            "timestamp",  Instant.now().toString()
        ));
        send(lmsUpdatesTopic, lrn, payload);
        LOG.info("Event LmsUpdated published: LRN={} CUST={}", lrn, customerId);
    }

    private void send(String topic, String key, String payload) {
        try {
            kafkaTemplate.send(topic, key, payload);
        } catch (Exception e) {
            // Event publishing failure must not block the business transaction
            LOG.warn("Failed to publish event to topic {} (suppressed): {}", topic, e.getMessage());
        }
    }

    private static String safeStr(String value) {
        return value != null ? value : "";
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
