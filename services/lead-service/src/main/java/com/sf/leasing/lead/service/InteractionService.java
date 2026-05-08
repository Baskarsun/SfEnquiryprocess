package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.LogInteractionRequest;
import com.sf.leasing.lead.domain.enums.InteractionType;
import com.sf.leasing.lead.domain.enums.LeadStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.model.Interaction;
import com.sf.leasing.lead.domain.model.Lead;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;

/**
 * Implements LP6: Interaction Logging & Next Action Scheduling.
 */
@ApplicationScoped
public class InteractionService {

    private static final Logger LOG = Logger.getLogger(InteractionService.class);

    @Inject
    EntityManager em;

    @Inject
    NotificationService notificationService;

    @Transactional
    public Interaction logInteraction(String lrn, LogInteractionRequest req, String userId) {

        Lead lead = Lead.findByLrn(lrn);
        if (lead == null) {
            throw new BusinessException("LEAD_NOT_FOUND", "Lead not found: " + lrn);
        }

        // Interaction logging is not permitted on closed leads (Rule LP6.3 / LP7.4)
        if (lead.isClosed()) {
            throw new BusinessException("LEAD_CLOSED", "Interaction logging is not permitted on a closed lead.");
        }

        // Rule LP6.1: Interaction type and timestamp are mandatory
        if (req.interactionType == null) {
            throw new BusinessException("INTERACTION_TYPE_REQUIRED", "Interaction type is mandatory.");
        }
        if (req.interactionTimestamp == null) {
            throw new BusinessException("INTERACTION_TIMESTAMP_REQUIRED", "Interaction timestamp is mandatory.");
        }
        if (isBlank(req.outcomeNotes)) {
            throw new BusinessException("OUTCOME_NOTES_REQUIRED", "Outcome notes are mandatory.");
        }

        // Rule LP6.2: For In-Progress leads, next action scheduling is mandatory before exit
        if (lead.status == LeadStatus.IN_PROGRESS) {
            if (req.nextActionDate == null || isBlank(req.nextActionMode)) {
                throw new BusinessException("NEXT_ACTION_REQUIRED",
                    "Next action date/time and mode are mandatory for In-Progress leads.");
            }
        }

        // Create immutable interaction record
        Interaction interaction = new Interaction();
        interaction.lead                 = lead;
        interaction.interactionType      = req.interactionType;
        interaction.interactionTimestamp = req.interactionTimestamp;
        interaction.outcomeNotes         = req.outcomeNotes;
        interaction.contactPerson        = req.contactPerson;
        interaction.contactDesignation   = req.contactDesignation;
        interaction.mode                 = req.mode;
        interaction.nextActionDate       = req.nextActionDate;
        interaction.nextActionMode       = req.nextActionMode;
        interaction.nextContactPerson    = req.nextContactPerson;
        interaction.reminderFlag         = req.reminderFlag;
        interaction.createdBy            = userId;
        interaction.createdAt            = LocalDateTime.now();
        interaction.persist();

        // Rule LP6.5: Increment attempt counters
        incrementAttemptCounter(lead, req.interactionType);

        // Transition lead status from ASSIGNED → IN_PROGRESS on first interaction
        if (lead.status == LeadStatus.ASSIGNED) {
            lead.status    = LeadStatus.IN_PROGRESS;
            lead.updatedBy = userId;
            lead.updatedAt = LocalDateTime.now();
        }

        // Rule LP6.4: Dispatch SMS asynchronously — failure never blocks the transaction
        notificationService.dispatchSmsAsync(lead.lrn, userId);

        LOG.infof("Interaction logged for LRN=%s by user=%s type=%s", lrn, userId, req.interactionType);
        return interaction;
    }

    private void incrementAttemptCounter(Lead lead, InteractionType type) {
        switch (type) {
            case PHONE_CALL  -> lead.callAttempts++;
            case EMAIL       -> lead.emailAttempts++;
            case WHATSAPP    -> lead.messageAttempts++;
            default          -> {} // IN_PERSON_MEETING, VIDEO_CALL: no counter
        }
        lead.updatedAt = LocalDateTime.now();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
