package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.model.ExceptionQueueRecord;
import com.sf.leasing.lead.infrastructure.persistence.ExceptionQueueRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Manages the Exception Queue per LP3.4 and EX-04.
 */
@Service
public class ExceptionQueueService {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionQueueService.class);
    private static final int DEFAULT_EXCEPTION_SLA_HOURS = 72;

    private final ExceptionQueueRepository exceptionQueueRepository;

    @PersistenceContext
    private EntityManager em;

    public ExceptionQueueService(ExceptionQueueRepository exceptionQueueRepository) {
        this.exceptionQueueRepository = exceptionQueueRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExceptionQueueRecord routeToExceptionQueue(String sourceType, String sourceIdentifier,
                                                       String reasonCode, String reasonDescription,
                                                       String fieldsInError, String rawData) {
        int slaHours = getExceptionSlaHours();

        ExceptionQueueRecord record = new ExceptionQueueRecord();
        record.sourceType        = sourceType;
        record.sourceIdentifier  = sourceIdentifier;
        record.reasonCode        = reasonCode;
        record.reasonDescription = reasonDescription;
        record.fieldsInError     = fieldsInError;
        record.rawData           = rawData;
        record.status            = "OPEN";
        record.ownedBy           = "CPU";
        record.cureSlaDeadline   = LocalDateTime.now().plusHours(slaHours);
        record.createdAt         = LocalDateTime.now();
        exceptionQueueRepository.save(record);

        LOG.info("Exception routed to queue: reason={} source={}", reasonCode, sourceIdentifier);
        return record;
    }

    @Transactional
    public void resolveException(UUID id, String resolvedBy, String resolutionNotes) {
        ExceptionQueueRecord record = exceptionQueueRepository.findById(id)
            .orElseThrow(() -> new BusinessException("EXCEPTION_NOT_FOUND", "Exception record not found: " + id));
        if ("RESOLVED".equals(record.status)) {
            throw new BusinessException("ALREADY_RESOLVED", "This exception has already been resolved.");
        }
        record.status          = "RESOLVED";
        record.resolvedBy      = resolvedBy;
        record.resolvedAt      = LocalDateTime.now();
        record.resolutionNotes = resolutionNotes;
        LOG.info("Exception {} resolved by {}", id, resolvedBy);
    }

    @Transactional
    public void escalateAgedExceptions() {
        List<Object[]> aged = em.createNativeQuery(
            "SELECT id FROM exception_queue WHERE status = 'OPEN' AND cure_sla_deadline < CURRENT_TIMESTAMP"
        ).getResultList();

        for (Object[] row : aged) {
            em.createNativeQuery(
                "UPDATE exception_queue SET status = 'ESCALATED' WHERE id = ?1"
            ).setParameter(1, row[0]).executeUpdate();
            LOG.info("Exception {} escalated due to SLA breach", row[0]);
        }
    }

    private int getExceptionSlaHours() {
        List<Object[]> rows = em.createNativeQuery(
            "SELECT threshold_hours FROM sla_config WHERE hierarchy_level = 'EXCEPTION_QUEUE'"
        ).getResultList();
        return rows.isEmpty() ? DEFAULT_EXCEPTION_SLA_HOURS : ((Number) rows.get(0)[0]).intValue();
    }
}
