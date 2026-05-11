package com.sf.leasing.lead.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SlaScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(SlaScheduler.class);

    private final AssignmentService assignmentService;
    private final ExceptionQueueService exceptionQueueService;
    private final TemperatureEngineService temperatureEngineService;

    public SlaScheduler(AssignmentService assignmentService,
                         ExceptionQueueService exceptionQueueService,
                         TemperatureEngineService temperatureEngineService) {
        this.assignmentService = assignmentService;
        this.exceptionQueueService = exceptionQueueService;
        this.temperatureEngineService = temperatureEngineService;
    }

    /** Rule LP5.4: Check SLA timers every hour and mark breaches. */
    @Scheduled(fixedRate = 3_600_000)
    public void checkSlaBreaches() {
        LOG.info("Running SLA breach check...");
        try {
            assignmentService.checkAndMarkSlaBreaches();
        } catch (Exception e) {
            LOG.error("SLA breach check failed: {}", e.getMessage());
        }
    }

    /** LP3.4: Escalate aged exception queue entries beyond cure SLA. */
    @Scheduled(fixedRate = 21_600_000)
    public void escalateAgedExceptions() {
        LOG.info("Running exception SLA escalation check...");
        try {
            exceptionQueueService.escalateAgedExceptions();
        } catch (Exception e) {
            LOG.error("Exception escalation check failed: {}", e.getMessage());
        }
    }

    /** LP7.2: Daily temperature degradation at 02:00. */
    @Scheduled(cron = "0 0 2 * * ?")
    public void runTemperatureDegradation() {
        LOG.info("Running daily temperature degradation...");
        try {
            temperatureEngineService.runDailyDegradation();
        } catch (Exception e) {
            LOG.error("Temperature degradation failed: {}", e.getMessage());
        }
    }
}
