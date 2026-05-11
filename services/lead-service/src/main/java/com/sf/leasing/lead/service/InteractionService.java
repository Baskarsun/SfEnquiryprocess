package com.sf.leasing.lead.service;

import com.sf.leasing.lead.api.dto.request.LogInteractionRequest;
import com.sf.leasing.lead.domain.enums.InteractionType;
import com.sf.leasing.lead.domain.enums.LeadStatus;
import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.model.Interaction;
import com.sf.leasing.lead.domain.model.Lead;
import com.sf.leasing.lead.infrastructure.persistence.InteractionRepository;
import com.sf.leasing.lead.infrastructure.persistence.LeadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implements LP6: Interaction Logging & Next Action Scheduling.
 */
@Service
public class InteractionService {

    private static final Logger LOG = LoggerFactory.getLogger(InteractionService.class);

    private final LeadRepository leadRepository;
    private final InteractionRepository interactionRepository;
    private final NotificationService notificationService;

    public InteractionService(LeadRepository leadRepository,
                               InteractionRepository interactionRepository,
                               NotificationService notificationService) {
        this.leadRepository = leadRepository;
        this.interactionRepository = interactionRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Interaction logInteraction(String lrn, LogInteractionRequest req, String userId) {

        Lead lead = leadRepository.findByLrn(lrn)
            .orElseThrow(() -> new BusinessException("LEAD_NOT_FOUND", "Lead not found: " + lrn));

        if (lead.isClosed()) {
            throw new BusinessException("LEAD_CLOSED", "Interaction logging is not permitted on a closed lead.");
        }

        if (req.interactionType == null) {
            throw new BusinessException("INTERACTION_TYPE_REQUIRED", "Interaction type is mandatory.");
        }
        if (req.interactionTimestamp == null) {
            throw new BusinessException("INTERACTION_TIMESTAMP_REQUIRED", "Interaction timestamp is mandatory.");
        }
        if (isBlank(req.outcomeNotes)) {
            throw new BusinessException("OUTCOME_NOTES_REQUIRED", "Outcome notes are mandatory.");
        }

        if (lead.status == LeadStatus.IN_PROGRESS) {
            if (req.nextActionDate == null || isBlank(req.nextActionMode)) {
                throw new BusinessException("NEXT_ACTION_REQUIRED",
                    "Next action date/time and mode are mandatory for In-Progress leads.");
            }
        }

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
        interactionRepository.save(interaction);

        incrementAttemptCounter(lead, req.interactionType);

        if (lead.status == LeadStatus.ASSIGNED) {
            lead.status    = LeadStatus.IN_PROGRESS;
            lead.updatedBy = userId;
            lead.updatedAt = LocalDateTime.now();
        }

        notificationService.dispatchSmsAsync(lead.lrn, userId);

        LOG.info("Interaction logged for LRN={} by user={} type={}", lrn, userId, req.interactionType);
        return interaction;
    }

    private void incrementAttemptCounter(Lead lead, InteractionType type) {
        switch (type) {
            case PHONE_CALL  -> lead.callAttempts++;
            case EMAIL       -> lead.emailAttempts++;
            case WHATSAPP    -> lead.messageAttempts++;
            default          -> {}
        }
        lead.updatedAt = LocalDateTime.now();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
