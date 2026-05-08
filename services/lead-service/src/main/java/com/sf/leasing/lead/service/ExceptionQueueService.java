package com.sf.leasing.lead.service;

import com.sf.leasing.lead.domain.exception.BusinessException;
import com.sf.leasing.lead.domain.model.ExceptionQueueRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Manages the Exception Queue per LP3.4 and EX-04.
 * SLA cure deadline: 72 hours (configurable via sla_config table).
 */
@ApplicationScoped
public class ExceptionQueueService {

    private static final Logger LOG = Logger.getLogger(ExceptionQueueService.class);
    private static final int DEFAULT_EXCEPTION_SLA_HOURS = 72;

    @Inject
    EntityManager em;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public ExceptionQueueRecord routeToExceptionQueue(String sourceType, String sourceIdentifier,
                                                       String reasonCode, String reasonDescription,
                                                       String fieldsInError, String rawData) {
        int slaHours = getExceptionSlaHours();

        ExceptionQueueRecord record = new ExceptionQueueRecord();
        record.sourceType       = sourceType;
        record.sourceIdentifier = sourceIdentifier;
        record.reasonCode       = reasonCode;
        record.reasonDescription = reasonDescription;
        record.fieldsInError    = fieldsInError;
        record.rawData          = rawData;
        record.status           = "OPEN";
        record.ownedBy          = "CPU";
        record.cureSlaDeadline  = LocalDateTime.now().plusHours(slaHours);
        record.createdAt        = LocalDateTime.now();
        record.persist();

        LOG.infof("Exception routed to queue: reason=%s source=%s", reasonCode, sourceIdentifier);
        return record;
    }

    @Transactional
    public void resolveException(UUID id, String resolvedBy, String resolutionNotes) {
        ExceptionQueueRecord record = ExceptionQueueRecord.findById(id);
        if (record == null) {
            throw new BusinessException("EXCEPTION_NOT_FOUND", "Exception record not found: " + id);
        }
        if ("RESOLVED".equals(record.status)) {
            throw new BusinessException("ALREADY_RESOLVED", "This exception has already been resolved.");
        }
        record.status          = "RESOLVED";
        record.resolvedBy      = resolvedBy;
        record.resolvedAt      = LocalDateTime.now();
        record.resolutionNotes = resolutionNotes;
        LOG.infof("Exception %s resolved by %s", id, resolvedBy);
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
            LOG.infof("Exception %s escalated due to SLA breach", row[0]);
        }
    }

    private int getExceptionSlaHours() {
        List<Object[]> rows = em.createNativeQuery(
            "SELECT threshold_hours FROM sla_config WHERE hierarchy_level = 'EXCEPTION_QUEUE'"
        ).getResultList();
        return rows.isEmpty() ? DEFAULT_EXCEPTION_SLA_HOURS : ((Number) rows.get(0)[0]).intValue();
    }
}
