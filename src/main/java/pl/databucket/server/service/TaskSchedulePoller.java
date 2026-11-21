package pl.databucket.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Looks for due task schedules every databucket.task-schedules.poll-interval (Settings > Schedules). */
@Component
@ConditionalOnProperty(name = "databucket.task-schedules.enabled", havingValue = "true", matchIfMissing = true)
public class TaskSchedulePoller {

    private static final Logger logger = LoggerFactory.getLogger(TaskSchedulePoller.class);

    private final TaskScheduleRunner runner;

    public TaskSchedulePoller(TaskScheduleRunner runner) {
        this.runner = runner;
    }

    @Scheduled(fixedDelayString = "${databucket.task-schedules.poll-interval:60s}", initialDelayString = "30s")
    public void poll() {
        try {
            runner.runDue();
        } catch (RuntimeException e) {
            logger.warn("Failed to run the due task schedules: {}", e.getMessage());
        }
    }
}
