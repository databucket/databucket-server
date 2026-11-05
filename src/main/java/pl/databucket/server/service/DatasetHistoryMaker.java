package pl.databucket.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import pl.databucket.server.dto.DatasetHistoryDto;
import pl.databucket.server.entity.Bucket;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Makes the history of the data of a dataset, in the transaction of its import, right after the rows are created:
 * the rows get their creation time ({@code since} before the start of the day of the import), then the changes are made in the order of their time.
 * Each change is an UPDATE of the row, so the triggers of the bucket write its history as for an edit in the
 * application; the time of the new history entry (the time of the transaction) is then set to the time of the change.
 * PostgreSQL only (triggers, jsonb_merge from schema.sql), like the bucket tables.
 */
@Component
public class DatasetHistoryMaker {

    @Autowired private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    /**
     * @param buckets  the buckets of the dataset with data, by name
     * @param tagIds   the new ids of the tags, by the ids in the dataset
     * @param importer the importing user, the author of a change without a user
     * @return the number of changes made
     */
    public int makeHistory(DatasetHistoryDto history, Map<String, Bucket> buckets, Map<Long, Long> tagIds, String importer)
            throws JsonProcessingException {
        // counted back from the start of the day of the import: times of a day (e.g. working hours) stay as in the file
        LocalDateTime now = LocalDate.now().atStartOfDay();
        Timestamp created = Timestamp.valueOf(now.minus(Duration.parse(history.getSince())));

        // the rows of a new table have ids in the order they were created: the position in the data of the dataset
        Map<String, List<Long>> rowIds = new HashMap<>();
        for (Bucket bucket : buckets.values()) {
            MapSqlParameterSource params = new MapSqlParameterSource("at", created);
            jdbcTemplate.update("UPDATE \"" + bucket.getTableName() + "\" SET created_at = :at, modified_at = :at", params);
            if (bucket.isHistory())
                jdbcTemplate.update("UPDATE \"" + bucket.getTableHistoryName() + "\" SET modified_at = :at", params);
            rowIds.put(bucket.getName(), jdbcTemplate.queryForList(
                    "SELECT data_id FROM \"" + bucket.getTableName() + "\" ORDER BY data_id", Map.of(), Long.class));
        }

        // the oldest first; changes of the same time in the order of the file
        List<DatasetHistoryDto.Change> changes = new ArrayList<>(history.getChanges());
        changes.sort(Comparator.comparing((DatasetHistoryDto.Change change) -> Duration.parse(change.getAgo())).reversed());

        for (DatasetHistoryDto.Change change : changes) {
            Bucket bucket = buckets.get(change.getBucket());
            String user = change.getUser() != null ? change.getUser() : importer;
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("id", rowIds.get(change.getBucket()).get(change.getRow()))
                    .addValue("user", user)
                    .addValue("at", Timestamp.valueOf(now.minus(Duration.parse(change.getAgo()))));

            StringBuilder set = new StringBuilder("modified_by = :user, modified_at = :at");
            if (change.getTagId() != null) {
                set.append(", tag_id = :tag");
                params.addValue("tag", change.getTagId() == 0 ? null : tagIds.get(change.getTagId()), java.sql.Types.BIGINT);
            }
            if (change.getReserved() != null) {
                set.append(", reserved = :reserved, reserved_by = :owner");
                params.addValue("reserved", change.getReserved());
                params.addValue("owner", change.getReserved() ? user : null, java.sql.Types.VARCHAR);
            }
            if (change.getProperties() != null) {
                set.append(", properties = jsonb_merge(properties, CAST(:properties AS jsonb))");
                params.addValue("properties", objectMapper.writeValueAsString(change.getProperties()));
            }
            jdbcTemplate.update("UPDATE \"" + bucket.getTableName() + "\" SET " + set + " WHERE data_id = :id", params);

            // the entry the trigger has just written (when the change changed something) has the time of the
            // transaction, the entries of earlier changes already have their own
            if (bucket.isHistory())
                jdbcTemplate.update("UPDATE \"" + bucket.getTableHistoryName() + "\" SET modified_at = :at"
                        + " WHERE data_id = :id AND modified_at = LOCALTIMESTAMP", params);
        }
        return changes.size();
    }
}
