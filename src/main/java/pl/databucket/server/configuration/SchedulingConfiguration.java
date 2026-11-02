package pl.databucket.server.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// background jobs, e.g. the nightly history cleanup (HistoryCleanupService)
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}
