package pl.databucket.server.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads the audit events: of one project (Settings > Activity) or of all (Management > Audit log), newest first, with
 * filters. The values of the filters are parameters only.
 */
@Service
public class AuditService {

    public static final int MAX_LIMIT = 200;
    /** The most events an export (CSV) gives. */
    public static final int MAX_EXPORT = 10_000;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    private final int retentionDays;

    public AuditService(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
                        @org.springframework.beans.factory.annotation.Value("${databucket.audit.retention-days:365}") int retentionDays) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.retentionDays = Math.max(0, retentionDays);
    }

    /**
     * @param projectId    the project to read; null = all events (management)
     * @param withoutProject management: also (only, with projectId null and this true) the events outside projects
     */
    public record Filter(Integer projectId, boolean withoutProject, String actor, String entityType, String action,
                         Instant from, Instant to) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Entry(long id, Instant at, Integer projectId, String projectName, String actor, String actorRole,
                        String source, String action, String entityType, String entityId, String entityName,
                        Map<String, Object> changes) {
    }

    public record Page(long total, int page, int limit, List<Entry> events) {
    }

    public record Facets(List<String> actors, List<String> types, List<String> actions, int retentionDays) {
    }

    public Page search(Filter filter, int page, int limit) {
        return search(filter, page, limit, MAX_LIMIT);
    }

    /** The newest events matching the filter, at most MAX_EXPORT (for a CSV file). */
    public Page export(Filter filter) {
        return search(filter, 1, MAX_EXPORT, MAX_EXPORT);
    }

    private Page search(Filter filter, int page, int limit, int maxLimit) {
        int pageNo = Math.max(1, page);
        int rows = Math.min(Math.max(1, limit), maxLimit);
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = where(filter, params);
        Long total = jdbcTemplate.queryForObject("SELECT count(*) FROM audit_events e" + where, params, Long.class);
        params.addValue("limit", rows).addValue("offset", (long) (pageNo - 1) * rows);
        List<Entry> events = jdbcTemplate.query("SELECT e.event_id, e.event_at, e.project_id, p.project_name, e.actor,"
                        + " e.actor_role, e.source, e.action, e.entity_type, e.entity_id, e.entity_name, e.changes::text AS changes"
                        + " FROM audit_events e LEFT JOIN projects p ON p.project_id = e.project_id" + where
                        + " ORDER BY e.event_id DESC LIMIT :limit OFFSET :offset",
                params, (rs, i) -> new Entry(rs.getLong("event_id"), rs.getTimestamp("event_at").toInstant(),
                        (Integer) rs.getObject("project_id"), rs.getString("project_name"), rs.getString("actor"),
                        rs.getString("actor_role"), rs.getString("source"), rs.getString("action"),
                        rs.getString("entity_type"), rs.getString("entity_id"), rs.getString("entity_name"),
                        changes(rs.getString("changes"))));
        return new Page(total != null ? total : 0, pageNo, rows, events);
    }

    /** Who changed something and the types of the objects changed, for the filters. */
    public Facets facets(Integer projectId) {
        MapSqlParameterSource params = new MapSqlParameterSource("p", projectId);
        String where = projectId != null ? " WHERE project_id = :p" : "";
        return new Facets(
                jdbcTemplate.queryForList("SELECT DISTINCT actor FROM audit_events" + where + " ORDER BY actor", params, String.class),
                jdbcTemplate.queryForList("SELECT DISTINCT entity_type FROM audit_events" + where + " ORDER BY entity_type", params, String.class),
                jdbcTemplate.queryForList("SELECT DISTINCT action FROM audit_events" + where + " ORDER BY action", params, String.class),
                retentionDays);
    }

    private static final java.util.regex.Pattern NOW = java.util.regex.Pattern.compile("@now(?:([+-])(\\d{1,6})([smhdw]))?");

    /**
     * A moment of a filter: ISO (2026-11-21T10:00:00Z) or relative as in the rules (@now, @now-1d, @now-2h; s, m, h,
     * d, w); null or blank = none.
     * @throws java.time.format.DateTimeParseException for anything else
     */
    public static Instant instant(String text) {
        if (text == null || text.isBlank())
            return null;
        java.util.regex.Matcher matcher = NOW.matcher(text.trim());
        if (!matcher.matches())
            return Instant.parse(text.trim());
        Instant now = Instant.now();
        if (matcher.group(1) == null)
            return now;
        java.time.Duration unit = switch (matcher.group(3)) {
            case "s" -> java.time.Duration.ofSeconds(1);
            case "m" -> java.time.Duration.ofMinutes(1);
            case "h" -> java.time.Duration.ofHours(1);
            case "d" -> java.time.Duration.ofDays(1);
            default -> java.time.Duration.ofDays(7);
        };
        java.time.Duration shift = unit.multipliedBy(Long.parseLong(matcher.group(2)));
        return "-".equals(matcher.group(1)) ? now.minus(shift) : now.plus(shift);
    }

    private static String where(Filter filter, MapSqlParameterSource params) {
        List<String> conditions = new ArrayList<>();
        if (filter.projectId() != null) {
            conditions.add("e.project_id = :project");
            params.addValue("project", filter.projectId());
        } else if (filter.withoutProject())
            conditions.add("e.project_id IS NULL");
        if (filter.actor() != null && !filter.actor().isBlank()) {
            conditions.add("e.actor = :actor");
            params.addValue("actor", filter.actor());
        }
        if (filter.entityType() != null && !filter.entityType().isBlank()) {
            conditions.add("e.entity_type = :type");
            params.addValue("type", filter.entityType());
        }
        if (filter.action() != null && !filter.action().isBlank()) {
            conditions.add("e.action = :action");
            params.addValue("action", filter.action());
        }
        if (filter.from() != null) {
            conditions.add("e.event_at >= :from");
            params.addValue("from", Timestamp.from(filter.from()));
        }
        if (filter.to() != null) {
            conditions.add("e.event_at < :to");
            params.addValue("to", Timestamp.from(filter.to()));
        }
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private Map<String, Object> changes(String json) {
        if (json == null)
            return null;
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            return null;
        }
    }
}
