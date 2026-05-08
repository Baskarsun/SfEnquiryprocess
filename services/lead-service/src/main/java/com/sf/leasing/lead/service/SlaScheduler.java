package com.sf.leasing.lead.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Scheduled jobs for SLA enforcement, exception queue management, and temperature degradation.
 * Phase 2 adds: daily temperature degradation (LP7.2).
 */
@ApplicationScoped
public class SlaScheduler {

    private static final Logger LOG = Logger.getLogger(SlaScheduler.class);

    @Inject
    AssignmentService assignmentService;

    @Inject
    ExceptionQueueService exceptionQueueService;

    @Inject
    TemperatureEngineService temperatureEngineService;

    /**
     * Rule LP5.4: Check SLA timers every hour and mark breaches.
     */
    @Scheduled(every = "1h", identity = "sla-breach-check")
    void checkSlaBreaches() {
        LOG.info("Running SLA breach check...");
        try {
            assignmentService.checkAndMarkSlaBreaches();
        } catch (Exception e) {
            LOG.errorf("SLA breach check failed: %s", e.getMessage());
        }
    }

    /**
     * LP3.4: Escalate aged exception queue entries beyond cure SLA.
     */
    @Scheduled(every = "6h", identity = "exception-sla-escalation")
    void escalateAgedExceptions() {
        LOG.info("Running exception SLA escalation check...");
        try {
            exceptionQueueService.escalateAgedExceptions();
        } catch (Exception e) {
            LOG.errorf("Exception escalation check failed: %s", e.getMessage());
        }
    }

    /**
     * LP7.2: Daily temperature degradation — Hot→Warm after hotWarnDays, Warm→Cold after warmColdDays.
     * Also surfaces leads exceeding the unanswered-attempts closure threshold.
     */
    @Scheduled(cron = "0 0 2 * * ?", identity = "temperature-degradation")
    void runTemperatureDegradation() {
        LOG.info("Running daily temperature degradation...");
        try {
            temperatureEngineService.runDailyDegradation();
        } catch (Exception e) {
            LOG.errorf("Temperature degradation failed: %s", e.getMessage());
        }
    }
}
