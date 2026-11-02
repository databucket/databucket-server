package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import pl.databucket.server.dto.OverviewBucketDto;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.BucketAccessRequest;
import pl.databucket.server.entity.User;
import pl.databucket.server.repository.BucketAccessRequestRepository;
import pl.databucket.server.repository.BucketRepository;
import pl.databucket.server.logging.AuditLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Overview page: the buckets of the project the user has access to, with the numbers of their data and the
 * size of their history, and the buckets visible on the Overview page the user has no access to, with their numbers
 * and the user's request for access. Buckets are project entities, so only the buckets of the signed-in project are
 * read (Hibernate tenant).
 */
@Service
public class OverviewService {

    // The history is worth clearing when it is bigger than this, or several times bigger than the data
    // (but not for small buckets, where the ratio means nothing)
    private final long historyWarningSize;
    private final double historyWarningRatio;
    private final long historyWarningRatioMinSize;

    private final BucketService bucketService;
    private final BucketRepository bucketRepository;
    private final BucketAccessRequestRepository requestRepository;
    private final UserService userService;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OverviewService(BucketService bucketService, BucketRepository bucketRepository,
                           BucketAccessRequestRepository requestRepository, UserService userService,
                           NamedParameterJdbcTemplate jdbcTemplate,
                           @Value("${databucket.overview.history-warning-size:500MB}") DataSize historyWarningSize,
                           @Value("${databucket.overview.history-warning-ratio:5}") double historyWarningRatio,
                           @Value("${databucket.overview.history-warning-ratio-min-size:50MB}") DataSize historyWarningRatioMinSize) {
        this.bucketService = bucketService;
        this.bucketRepository = bucketRepository;
        this.requestRepository = requestRepository;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
        this.historyWarningSize = historyWarningSize.toBytes();
        this.historyWarningRatio = historyWarningRatio;
        this.historyWarningRatioMinSize = historyWarningRatioMinSize.toBytes();
    }

    // the user is read in the same transaction as the buckets: access is checked on their users and teams,
    // which match only as the same entities
    @Transactional(readOnly = true)
    public List<OverviewBucketDto> getOverview() {
        User user = userService.getCurrentUser();
        // the user's latest request for each bucket
        Map<Long, BucketAccessRequest> requests = new HashMap<>();
        for (BucketAccessRequest request : requestRepository.findAllByUserOrderByRequestedAt(user))
            requests.put(request.getBucket().getId(), request);

        List<OverviewBucketDto> overview = new ArrayList<>();
        for (Bucket bucket : bucketRepository.findAllByDeletedOrderById(false)) {
            if (bucketService.hasUserAccessToBucket(bucket, user))
                overview.add(describe(bucket, user));
            else if (bucket.isOverviewVisible())
                overview.add(describeWithoutAccess(bucket, requests.get(bucket.getId())));
        }
        return overview;
    }

    private OverviewBucketDto describeWithoutAccess(Bucket bucket, BucketAccessRequest request) {
        OverviewBucketDto dto = identity(bucket);
        Map<String, Object> counts = jdbcTemplate.queryForMap(
                "SELECT count(*) AS total, count(*) FILTER (WHERE NOT reserved) AS free FROM " + quote(bucket.getTableName()),
                Map.of());
        dto.setRows(((Number) counts.get("total")).longValue());
        dto.setFree(((Number) counts.get("free")).longValue());
        // an approved request is shown as access; a request for a bucket the access was taken from again is history
        if (request != null && request.getStatus() != BucketAccessRequest.Status.APPROVED) {
            dto.setRequestStatus(request.getStatus().name());
            dto.setRequestedAt(request.getStatus() == BucketAccessRequest.Status.PENDING ? request.getRequestedAt() : request.getDecidedAt());
        }
        return dto;
    }

    private static OverviewBucketDto identity(Bucket bucket) {
        OverviewBucketDto dto = new OverviewBucketDto();
        dto.setId(bucket.getId());
        dto.setName(bucket.getName());
        dto.setDescription(bucket.getDescription());
        dto.setIconName(bucket.getIconName());
        dto.setIconColor(bucket.getIconColor());
        dto.setIconSvg(bucket.getIconSvg());
        return dto;
    }

    private OverviewBucketDto describe(Bucket bucket, User user) {
        OverviewBucketDto dto = identity(bucket);
        dto.setAccess(true);
        dto.setHistory(bucket.isHistory());

        // table names come from the bucket id, not from the user
        String data = quote(bucket.getTableName());
        String history = quote(bucket.getTableHistoryName());

        Map<String, Object> counts = jdbcTemplate.queryForMap(
                "SELECT count(*) AS total,"
                        + " count(*) FILTER (WHERE NOT reserved) AS free,"
                        + " count(*) FILTER (WHERE reserved AND reserved_by = :user) AS mine,"
                        + " max(coalesce(modified_at, created_at)) AS last"
                        + " FROM " + data,
                new MapSqlParameterSource("user", user.getUsername()));
        long rows = ((Number) counts.get("total")).longValue();
        long free = ((Number) counts.get("free")).longValue();
        long mine = ((Number) counts.get("mine")).longValue();
        dto.setRows(rows);
        dto.setFree(free);
        dto.setReservedByMe(mine);
        dto.setReservedByOthers(rows - free - mine);
        dto.setLastModifiedAt((Date) counts.get("last"));

        // sizes and the number of history entries come from the statistics, without reading the history
        Map<String, Object> sizes = jdbcTemplate.queryForMap(
                "SELECT pg_total_relation_size(:data::regclass) AS data_size,"
                        + " pg_total_relation_size(:history::regclass) AS history_size,"
                        + " (SELECT reltuples FROM pg_class WHERE oid = :history::regclass) AS history_entries",
                new MapSqlParameterSource().addValue("data", data).addValue("history", history));
        long dataSize = ((Number) sizes.get("data_size")).longValue();
        long historySize = ((Number) sizes.get("history_size")).longValue();
        long historyEntries = ((Number) sizes.get("history_entries")).longValue();
        // -1: the table has never been analyzed (new or small): count it
        if (historyEntries < 0)
            historyEntries = jdbcTemplate.queryForObject("SELECT count(*) FROM " + history, Map.of(), Long.class);
        dto.setDataSize(dataSize);
        dto.setHistorySize(historySize);
        dto.setHistoryEntries(historyEntries);
        dto.setHistoryLarge(historySize > historyWarningSize
                || (historySize > historyWarningRatioMinSize && historySize > dataSize * historyWarningRatio));
        return dto;
    }

    /**
     * Removes the whole history of the bucket and gives its space back at once (TRUNCATE, not DELETE).
     *
     * @return the bytes freed
     */
    public long clearHistory(Bucket bucket) {
        String history = quote(bucket.getTableHistoryName());
        long size = jdbcTemplate.queryForObject("SELECT pg_total_relation_size(:history::regclass)",
                new MapSqlParameterSource("history", history), Long.class);
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE " + history);
        AuditLog.log("data.history-clear", "bucket={} all freed={}", bucket.getName(), DataSize.ofBytes(size).toMegabytes() + "MB");
        return size;
    }

    private static String quote(String tableName) {
        return "\"" + tableName + "\"";
    }
}
