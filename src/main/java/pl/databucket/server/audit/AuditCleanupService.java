package pl.databucket.server.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import pl.databucket.server.logging.AuditLog;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/**
 * Keeps the audit table within databucket.audit.retention-days: every night the older events are removed, in batches
 * (short transactions). With several instances one of them cleans at a time (an advisory lock), the others skip it.
 */
@Service
@ConditionalOnProperty(name = "databucket.audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(AuditCleanupService.class);

    // the advisory lock of the cleanup: "DBAC"
    private static final long LOCK_KEY = 0x44424143L;
    static final int BATCH = 10_000;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final int retentionDays;

    public AuditCleanupService(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate,
                               @Value("${databucket.audit.retention-days:365}") int retentionDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.retentionDays = retentionDays;
    }

    /** How long the events are kept; 0 = for ever. */
    public int getRetentionDays() {
        return Math.max(0, retentionDays);
    }

    @Scheduled(cron = "${databucket.audit.cleanup-cron:0 45 2 * * *}")
    public void cleanNightly() {
        try {
            clean(Instant.now());
        } catch (RuntimeException e) {
            logger.warn("Failed to remove the old audit events: {}", e.getMessage());
        }
    }

    /** @return the number of events removed; -1 when another instance is cleaning */
    public long clean(Instant now) {
        if (getRetentionDays() == 0)
            return 0;
        Timestamp before = Timestamp.from(now.minus(Duration.ofDays(getRetentionDays())));
        long removed = 0;
        while (true) {
            Integer batch = transactionTemplate.execute(status -> {
                Boolean locked = jdbcTemplate.queryForObject("SELECT pg_try_advisory_xact_lock(:key)",
                        new MapSqlParameterSource("key", LOCK_KEY), Boolean.class);
                if (!Boolean.TRUE.equals(locked))
                    return -1;
                return jdbcTemplate.update("DELETE FROM audit_events WHERE event_id IN (SELECT event_id FROM audit_events"
                                + " WHERE event_at < :before ORDER BY event_id LIMIT " + BATCH + ")",
                        new MapSqlParameterSource("before", before));
            });
            if (batch == null || batch < 0)
                return removed == 0 ? -1 : removed;
            removed += batch;
            if (batch < BATCH)
                break;
        }
        if (removed > 0)
            AuditLog.log("audit.cleanup", "removed={} days={}", removed, getRetentionDays());
        return removed;
    }
}
