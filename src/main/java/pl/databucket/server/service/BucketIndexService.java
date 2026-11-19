package pl.databucket.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pl.databucket.server.dto.IndexedPropertyDto;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.service.data.filter.PropertyIndex;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The indexes of the indexed properties of a bucket ({@link IndexedPropertyDto}) on its data table, named
 * "x-bucket-N-p-&lt;hash of the type and path&gt;" ({@link PropertyIndex}).
 */
@Service
public class BucketIndexService {

    private static final Logger logger = LoggerFactory.getLogger(BucketIndexService.class);

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /** Checks the indexed properties of a bucket: at most 5, valid paths and types, each path once. */
    public static void check(List<IndexedPropertyDto> properties) {
        if (properties == null)
            return;
        if (properties.size() > 5)
            throw new IllegalArgumentException("A bucket can have at most 5 indexed properties");
        Set<String> paths = new HashSet<>();
        for (IndexedPropertyDto property : properties) {
            if (property == null || property.getPath() == null || !property.getPath().matches(IndexedPropertyDto.PATH_PATTERN))
                throw new IllegalArgumentException("Invalid indexed property path: " + (property == null ? null : property.getPath())
                        + " (a property like $.a.b; letters, digits, _, - and spaces)");
            if (!IndexedPropertyDto.TEXT.equals(property.getType()) && !IndexedPropertyDto.NUMBER.equals(property.getType()))
                throw new IllegalArgumentException("The type of the indexed property " + property.getPath() + " must be text or number");
            if (!paths.add(property.getPath()))
                throw new IllegalArgumentException("The property " + property.getPath() + " is indexed twice");
        }
    }

    /** Creates the indexes on a new (empty) table, inside the transaction that creates it. */
    public void createIndexes(String table, List<IndexedPropertyDto> properties) {
        if (properties != null)
            for (IndexedPropertyDto property : properties)
                execute(PropertyIndex.createSql(table, property, false));
    }

    /**
     * Makes the indexes of the table match the properties: removes the other property indexes (and invalid ones,
     * left by a failed build) and creates the missing ones. Runs outside a transaction (after the bucket was saved),
     * as it builds and removes them CONCURRENTLY, without blocking the work on the data; on a big table this takes
     * a while.
     */
    public void syncIndexes(String table, List<IndexedPropertyDto> properties) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("The indexes are built CONCURRENTLY, which can't run in a transaction");

        Map<String, IndexedPropertyDto> wanted = new LinkedHashMap<>();
        if (properties != null)
            for (IndexedPropertyDto property : properties)
                wanted.put(PropertyIndex.indexName(table, property), property);

        Map<String, Boolean> existing = existingIndexes(table);
        for (Map.Entry<String, Boolean> index : existing.entrySet())
            if (!wanted.containsKey(index.getKey()) || !index.getValue()) {
                execute("DROP INDEX CONCURRENTLY IF EXISTS \"" + index.getKey() + "\"");
                AuditLog.log("bucket.index-drop", "table={} index={}", table, index.getKey());
            }

        boolean created = false;
        for (Map.Entry<String, IndexedPropertyDto> index : wanted.entrySet())
            if (!Boolean.TRUE.equals(existing.get(index.getKey()))) {
                long start = System.currentTimeMillis();
                execute(PropertyIndex.createSql(table, index.getValue(), true));
                created = true;
                AuditLog.log("bucket.index-create", "table={} index={} path={} type={} ms={}", table, index.getKey(),
                        index.getValue().getPath(), index.getValue().getType(), System.currentTimeMillis() - start);
            }

        // the statistics of the new index expressions: without them the planner guesses 0.5% of the rows match and
        // e.g. for ORDER BY id LIMIT 20 scans the primary key instead of the index (1M rows: 300 ms instead of 20)
        if (created)
            execute("ANALYZE \"" + table + "\"");
    }

    /** The property indexes of the table: name -> valid (false: a failed CONCURRENTLY build). */
    Map<String, Boolean> existingIndexes(String table) {
        String sql = "SELECT c.relname AS name, i.indisvalid AS valid FROM pg_index i"
                + " JOIN pg_class c ON c.oid = i.indexrelid JOIN pg_class t ON t.oid = i.indrelid"
                + " WHERE t.relname = :table AND left(c.relname, :length) = :prefix";
        String prefix = PropertyIndex.namePrefix(table);
        Map<String, Boolean> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, Map.of("table", table, "length", prefix.length(), "prefix", prefix),
                rs -> {
                    result.put(rs.getString("name"), rs.getBoolean("valid"));
                });
        return result;
    }

    private void execute(String sql) {
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }
}
