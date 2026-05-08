package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.response.CustomerResponse;
import com.sf.leasing.lead.domain.enums.CamStatus;
import com.sf.leasing.lead.domain.enums.ProspectStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.exception.ErrorCodes;
import com.sf.leasing.lead.domain.model.Application;
import com.sf.leasing.lead.domain.model.Customer;
import com.sf.leasing.lead.domain.model.DownstreamEventLog;
import com.sf.leasing.lead.domain.model.Lineage;
import com.sf.leasing.lead.domain.enums.OpportunityStatus;
import com.sf.leasing.lead.domain.model.Opportunity;
import com.sf.leasing.lead.domain.model.Prospect;
import com.sf.leasing.lead.infrastructure.locking.RedisSequenceGenerator;
import com.sf.leasing.lead.infrastructure.messaging.LeadEventProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * PP8.1 — Enterprise Customer Creation Gate.
 *
 * Business rules:
 *   PP8.1: KYC = Complete AND CAM = Approved must both be true simultaneously.
 *          Only then is a Customer record created and CUST-YYYY-NNNNNN issued.
 *   PP8.2: Full lineage chain updated: Lead → Prospect → Opp → Quote → App → Customer.
 *   PP8.4: CustomerCreated Kafka event published to downstream systems.
 *          LmsUpdated event dispatched for Lead Management System sync (LP8.9).
 */
@ApplicationScoped
public class CustomerCreationService {

    private static final Logger LOG = Logger.getLogger(CustomerCreationService.class);

    private static final List<String> VALID_STATUSES_FOR_CREATION =
        List.of("ACTIVE", "IN_APPRAISAL");

    @Inject
    RedisSequenceGenerator sequenceGenerator;

    @Inject
    LeadEventProducer eventProducer;

    @Inject
    NotificationService notificationService;

    @Inject
    EntityManager em;

    // -------------------------------------------------------
    // PP8.1: Create enterprise customer
    // -------------------------------------------------------

    @Transactional
    public Customer createCustomer(String applicationId, String operatorId) {

        // 1. Resolve application
        Application app = Application.findByApplicationId(applicationId);
        if (app == null) {
            throw new BusinessException(ErrorCodes.APPLICATION_NOT_FOUND,
                "Application not found: " + applicationId);
        }

        // 2. Resolve prospect
        Prospect prospect = Prospect.find("id", app.prospectUuid).firstResult();
        if (prospect == null) {
            throw new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND,
                "Prospect not found for application: " + applicationId);
        }

        // 3. Guard: customer must not already exist for this prospect
        Customer existing = Customer.findByProspectUuid(prospect.id);
        if (existing != null) {
            throw new BusinessException(ErrorCodes.CUSTOMER_ALREADY_EXISTS,
                "Customer already created for prospect: " + prospect.prospectId);
        }

        // 4. KYC + CAM gate (PP8.1) — both must be satisfied simultaneously
        boolean kycComplete  = "COMPLETE".equalsIgnoreCase(app.kycStatus);
        boolean camApproved  = app.camStatus == CamStatus.APPROVED;

        if (!kycComplete && !camApproved) {
            CustomerResponse.GateStatus gs = buildGateStatus(kycComplete, camApproved);
            throw new BusinessException(ErrorCodes.CUSTOMER_GATE_KYC_INCOMPLETE,
                "Customer gate not satisfied: KYC=" + app.kycStatus + ", CAM=" + app.camStatus);
        }
        if (!kycComplete) {
            throw new BusinessException(ErrorCodes.CUSTOMER_GATE_KYC_INCOMPLETE,
                "KYC is not complete (current: " + app.kycStatus + "). Customer creation blocked.");
        }
        if (!camApproved) {
            throw new BusinessException(ErrorCodes.CUSTOMER_GATE_CAM_NOT_APPROVED,
                "CAM is not approved (current: " + app.camStatus + "). Customer creation blocked.");
        }

        // 5. Generate CUST-YYYY-NNNNNN
        String customerId = sequenceGenerator.generateCustomerId();
        LocalDateTime now = LocalDateTime.now();

        // 6. Build customer master from validated prospect data
        Customer customer = new Customer();
        customer.customerId          = customerId;
        customer.prospectUuid        = prospect.id;
        customer.prospectBusinessId  = prospect.prospectId;
        customer.applicationUuid     = app.id;
        customer.applicationId       = app.applicationId;
        customer.leadType            = prospect.leadType;
        customer.legalName           = prospect.legalName != null ? prospect.legalName : "";
        customer.pan                 = prospect.pan;
        customer.gstin               = prospect.gstin;
        customer.registeredAddress   = prospect.registeredAddress;
        customer.registeredPincode   = prospect.registeredPincode;
        customer.ucic                = prospect.ucic;
        customer.kycCompletedAt      = now;
        customer.camApprovedAt       = now;
        customer.sanctionId          = app.sanctionId;
        customer.sanctionPackage     = app.sanctionPackage;
        customer.status              = "ACTIVE";
        customer.createdBy           = operatorId;
        customer.createdAt           = now;
        customer.persist();

        // 7. Advance prospect status to CUSTOMER_CREATED
        prospect.status    = ProspectStatus.CUSTOMER_CREATED;
        prospect.updatedBy = operatorId;
        prospect.updatedAt = now;

        // 8. Update full lineage chain (PP8.2)
        updateLineageOnCustomerCreation(prospect, app, customer, now);

        // 9. Update opportunities linked to this prospect to CLOSED (OpportunityWon)
        markOpportunitiesWon(prospect.id, customerId, now, operatorId);

        // 10. Publish downstream events (non-blocking)
        publishDownstreamEvents(customer, prospect, app);

        LOG.infof("Customer created: CUST=%s for PROSPECT=%s APP=%s by=%s",
            customerId, prospect.prospectId, applicationId, operatorId);

        return customer;
    }

    // -------------------------------------------------------
    // PP8.1: Gate status — for UI blocked display
    // -------------------------------------------------------

    public CustomerResponse.GateStatus getGateStatus(String applicationId) {
        Application app = Application.findByApplicationId(applicationId);
        if (app == null) {
            throw new BusinessException(ErrorCodes.APPLICATION_NOT_FOUND,
                "Application not found: " + applicationId);
        }
        boolean kycComplete = "COMPLETE".equalsIgnoreCase(app.kycStatus);
        boolean camApproved = app.camStatus == CamStatus.APPROVED;
        return buildGateStatus(kycComplete, camApproved);
    }

    // -------------------------------------------------------
    // Query
    // -------------------------------------------------------

    public Customer getByCustomerId(String customerId) {
        Customer c = Customer.findByCustomerId(customerId);
        if (c == null) {
            throw new BusinessException(ErrorCodes.CUSTOMER_NOT_FOUND,
                "Customer not found: " + customerId);
        }
        return c;
    }

    public Customer getByProspectId(String prospectBusinessId) {
        Prospect p = Prospect.findByProspectId(prospectBusinessId);
        if (p == null) {
            throw new BusinessException(ErrorCodes.PROSPECT_NOT_FOUND,
                "Prospect not found: " + prospectBusinessId);
        }
        Customer c = Customer.findByProspectUuid(p.id);
        if (c == null) {
            throw new BusinessException(ErrorCodes.CUSTOMER_NOT_FOUND,
                "No customer created yet for prospect: " + prospectBusinessId);
        }
        return c;
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    private void updateLineageOnCustomerCreation(Prospect prospect, Application app,
                                                  Customer customer, LocalDateTime now) {
        Lineage lineage = Lineage.findByProspectUuid(prospect.id);
        if (lineage == null) {
            LOG.warnf("Lineage record not found for prospect=%s; cannot update chain.", prospect.id);
            return;
        }
        lineage.customerUuid    = customer.id;
        lineage.customerId      = customer.customerId;
        lineage.applicationUuid = app.id;
        lineage.applicationBusinessId = app.applicationId;
        lineage.updatedAt       = now;
    }

    @SuppressWarnings("unchecked")
    private void markOpportunitiesWon(UUID prospectUuid, String customerId,
                                       LocalDateTime now, String operatorId) {
        List<Opportunity> opps = Opportunity.list("prospectUuid", prospectUuid);
        for (Opportunity opp : opps) {
            if (opp.status == OpportunityStatus.OPEN || opp.status == OpportunityStatus.SANCTIONED) {
                opp.status    = OpportunityStatus.WON;
                opp.updatedBy = operatorId;
                opp.updatedAt = now;
                eventProducer.publishOpportunityWon(opp.opportunityId,
                    opp.prospectBusinessId, customerId);
                logDownstreamEvent("OpportunityWon", "OPPORTUNITY", opp.opportunityId,
                    "leasing.opportunity.events");
            }
        }
    }

    private void publishDownstreamEvents(Customer customer, Prospect prospect, Application app) {
        // CustomerCreated — consumed by Contract, Billing, and Asset Delivery services
        try {
            eventProducer.publishCustomerCreated(
                customer.customerId,
                customer.prospectBusinessId,
                customer.applicationId,
                customer.createdBy
            );
            logDownstreamEvent("CustomerCreated", "CUSTOMER", customer.customerId,
                "leasing.customer.events");
        } catch (Exception e) {
            LOG.warnf("CustomerCreated event publish failed (suppressed): %s", e.getMessage());
            logDownstreamEventFailed("CustomerCreated", "CUSTOMER", customer.customerId, e.getMessage());
        }

        // LmsUpdated — sync Lead Management System record (LP8.9)
        try {
            eventProducer.publishLmsUpdated(
                prospect.leadLrn,
                customer.prospectBusinessId,
                customer.customerId
            );
            logDownstreamEvent("LmsUpdated", "CUSTOMER", customer.customerId,
                "leasing.lms.updates");
        } catch (Exception e) {
            LOG.warnf("LmsUpdated event publish failed (suppressed): %s", e.getMessage());
            logDownstreamEventFailed("LmsUpdated", "CUSTOMER", customer.customerId, e.getMessage());
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void logDownstreamEvent(String eventType, String entityType, String entityId, String topic) {
        DownstreamEventLog log = new DownstreamEventLog();
        log.eventType   = eventType;
        log.entityType  = entityType;
        log.entityId    = entityId;
        log.topic       = topic;
        log.status      = "PUBLISHED";
        log.publishedAt = LocalDateTime.now();
        log.persist();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void logDownstreamEventFailed(String eventType, String entityType, String entityId, String error) {
        DownstreamEventLog log = new DownstreamEventLog();
        log.eventType    = eventType;
        log.entityType   = entityType;
        log.entityId     = entityId;
        log.status       = "FAILED";
        log.errorMessage = error;
        log.publishedAt  = LocalDateTime.now();
        log.persist();
    }

    private CustomerResponse.GateStatus buildGateStatus(boolean kycComplete, boolean camApproved) {
        CustomerResponse.GateStatus gs = new CustomerResponse.GateStatus();
        gs.kycComplete  = kycComplete;
        gs.camApproved  = camApproved;
        if (!kycComplete && !camApproved) {
            gs.blockReason = "Both KYC and CAM must be complete before customer creation.";
        } else if (!kycComplete) {
            gs.blockReason = "KYC documents are not yet complete.";
        } else {
            gs.blockReason = "CAM has not been approved yet.";
        }
        return gs;
    }
}
