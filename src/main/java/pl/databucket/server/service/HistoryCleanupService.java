package pl.databucket.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import pl.databucket.server.logging.AuditLog;

import java.util.List;
import java.util.Map;

/**
 * Keeps the history of the buckets with a history limit within it: every night the history entries of each data row
 * beyond the latest N are removed, except the entry of the creation of the row. It runs for all projects, so it reads
 * the buckets with SQL (no signed-in project). With several instances of Databucket a bucket is cleaned by one of
 * them at a time (an advisory lock), the others skip it.
 */
@Service
@ConditionalOnProperty(name = "databucket.history-cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class HistoryCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(HistoryCleanupService.class);

    // the first key of the advisory locks of the cleanup (the second is the bucket id)
    private static final int LOCK_KEY = 0x44424843; // "DBHC"

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public HistoryCleanupService(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(cron = "${databucket.history-cleanup.cron:0 30 2 * * *}")
    public void cleanAll() {
        List<Map<String, Object>> buckets = jdbcTemplate.queryForList(
                "SELECT bucket_id, bucket_name, history_limit FROM buckets"
                        + " WHERE deleted = false AND history_limit IS NOT NULL AND history_limit > 0 ORDER BY bucket_id",
                Map.of());
        for (Map<String, Object> bucket : buckets) {
            long id = ((Number) bucket.get("bucket_id")).longValue();
            try {
                clean(id, (String) bucket.get("bucket_name"), ((Number) bucket.get("history_limit")).intValue());
            } catch (Exception e) {
                logger.warn("Failed to limit the history of the bucket {}: {}", bucket.get("bucket_name"), e.getMessage());
            }
        }
    }

    /**
     * @return the number of history entries removed, -1 when another instance is cleaning the bucket
     */
    public int clean(long bucketId, String bucketName, int limit) {
        // the table name comes from the bucket id, not from a user
        String history = "\"x-bucket-" + bucketId + "-h\"";
        Integer removed = transactionTemplate.execute(status -> {
            Boolean locked = jdbcTemplate.queryForObject("SELECT pg_try_advisory_xact_lock(:key, :bucket)",
                    new MapSqlParameterSource().addValue("key", LOCK_KEY).addValue("bucket", (int) bucketId), Boolean.class);
            if (!Boolean.TRUE.equals(locked))
                return -1;
            return jdbcTemplate.update(
                    "DELETE FROM " + history + " h USING ("
                            + " SELECT id,"
                            + " row_number() OVER (PARTITION BY data_id ORDER BY id DESC) AS newest,"
                            + " min(id) OVER (PARTITION BY data_id) AS created"
                            + " FROM " + history + ") r"
                            + " WHERE h.id = r.id AND r.newest > :limit AND r.id <> r.created",
                    new MapSqlParameterSource("limit", limit));
        });
        if (removed != null && removed > 0)
            AuditLog.log("data.history-limit", "bucket={} limit={} removed={}", bucketName, limit, removed);
        return removed == null ? 0 : removed;
    }
}
