package pl.databucket.server.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What an audit event records of a change: property -> [before, after], only the properties that changed (a new object:
 * its values, before = null). Values as JSON (dates as ISO text, objects of jsonb columns as JSON, references as
 * {id, name}). Left out: the audit columns and the id, the fields that change by themselves (e.g. the next run of a
 * schedule) and, as "***", the values of secrets (password, secret, token, hash). Pure, without Hibernate: the
 * listener gives the properties.
 */
public final class AuditChanges {

    public static final String MASK = "***";

    /** A property of the object: its name, the value before and after (before = null for a new object). */
    public record Property(String name, Object before, Object after) {
    }

    private static final Set<String> ALWAYS_IGNORED = Set.of("id", "projectId", "deleted", "createdBy", "createdAt",
            "modifiedBy", "modifiedAt");

    // fields that change without anybody changing the object
    private static final Map<String, Set<String>> IGNORED = Map.of(
            "User", Set.of("lastSendEmailForgotPasswordLinkDate", "lastSendEmailTempPasswordDate"),
            "TaskSchedule", Set.of("nextRunAt", "lastRunAt", "lastStatus", "lastCount", "lastMessage", "failures"));

    private static final Pattern SECRET = Pattern.compile("(?i).*(password|secret|token|hash).*");

    // values of one property longer than this (as JSON) are recorded only as their length
    static final int MAX_VALUE_LENGTH = 4000;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private AuditChanges() {
    }

    /** The changes to record; empty when nothing worth recording changed. */
    public static Map<String, Object> of(String entityType, List<Property> properties) {
        Set<String> ignored = IGNORED.getOrDefault(entityType, Set.of());
        Map<String, Object> changes = new LinkedHashMap<>();
        for (Property property : properties) {
            if (ALWAYS_IGNORED.contains(property.name()) || ignored.contains(property.name()))
                continue;
            Object before = json(property.before());
            Object after = json(property.after());
            if (Objects.equals(before, after))
                continue;
            // a flag such as changePassword says nothing secret
            if (SECRET.matcher(property.name()).matches() && !(before instanceof Boolean) && !(after instanceof Boolean)) {
                before = before == null ? null : MASK;
                after = after == null ? null : MASK;
            }
            changes.put(property.name(), Arrays.asList(limit(before), limit(after)));
        }
        return changes;
    }

    /** A value as JSON data: text, number, boolean, null, map or list. */
    public static Object json(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean)
            return value;
        if (value instanceof Date date)
            return Instant.ofEpochMilli(date.getTime()).toString();
        if (value instanceof TemporalAccessor)
            return value.toString();
        if (value instanceof Enum<?> e)
            return e.name();
        return MAPPER.convertValue(value, Object.class);
    }

    private static Object limit(Object value) {
        if (value == null || value instanceof Number || value instanceof Boolean)
            return value;
        try {
            int length = value instanceof String s ? s.length() : MAPPER.writeValueAsString(value).length();
            return length > MAX_VALUE_LENGTH ? "(" + length + " characters)" : value;
        } catch (Exception e) {
            return "(not readable)";
        }
    }

    static String toJson(Map<String, Object> changes) {
        try {
            return MAPPER.writeValueAsString(changes);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
