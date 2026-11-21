package pl.databucket.server.audit;

import org.slf4j.helpers.MessageFormatter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The operations of the operation log ({@link pl.databucket.server.logging.AuditLog}) that also go to the audit table:
 * sign-ins and account events, project clearing and datasets, bucket indexes, task runs and the bulk operations on
 * data (a row per call with the count, not per data row). The creating, changing and deleting of objects is left out:
 * {@link AuditListener} records them with their changes. Pure: which operation, its object and details.
 */
public final class AuditActions {

    /** An operation to record: the type and name of its object, its project (if it names one) and its details. */
    public record Action(String entityType, String entityName, Integer projectId, Map<String, Object> details) {
    }

    private static final Pattern PAIR = Pattern.compile("([A-Za-z][\\w-]*)=(.*?)(?=\\s[A-Za-z][\\w-]*=|$)");

    private AuditActions() {
    }

    /**
     * @param source where it came from (AuditContext.source): the task runs of the scheduler are in its own history
     * @return null when the operation isn't recorded
     */
    public static Action of(String action, String message, Object[] args, String source) {
        String type = type(action, message, source);
        if (type == null)
            return null;
        Map<String, Object> details = details(message, args);
        Object name = first(details, "name", "user", "bucket", "table", "email", "identity");
        Integer projectId = action.startsWith("project.") ? number(details.get("id"))
                : action.startsWith("auth.") && details.containsKey("project") ? number(details.get("project")) : null;
        return new Action(type, name != null ? String.valueOf(name) : null, projectId, details);
    }

    private static String type(String action, String message, String source) {
        if (action.startsWith("auth.") && !action.equals("auth.sso-registration-offered"))
            return "Account";
        return switch (action) {
            case "user.registered" -> "User";
            case "access.request", "bucket.index-create", "bucket.index-drop" -> "Bucket";
            // the bulk ones: an insert of one row is data, not a change of the settings
            case "data.create" -> message.contains("count=") ? "Data" : null;
            case "data.delete", "data.history-clear", "data.history-limit" -> "Data";
            case "task.run" -> "SCHEDULER".equals(source) ? null : "Task";
            case "task.schedule-disabled" -> "Schedule";
            case "project.clear", "project.dataset-import", "project.dataset-export" -> "Project";
            case "audit.cleanup" -> "Audit";
            default -> null;
        };
    }

    /** The key=value pairs of the message, e.g. "bucket={} count={}" -> {bucket: orders, count: 12}. */
    static Map<String, Object> details(String message, Object[] args) {
        String text = MessageFormatter.arrayFormat(message, args).getMessage();
        Map<String, Object> details = new LinkedHashMap<>();
        Matcher matcher = PAIR.matcher(text);
        while (matcher.find()) {
            String value = matcher.group(2).trim();
            Object parsed;
            if (value.isEmpty() || value.equals("null"))
                parsed = null;
            else if (value.matches("0|-?[1-9]\\d{0,8}"))
                parsed = Integer.parseInt(value); // a number, not a text like 007
            else
                parsed = value;
            details.put(matcher.group(1), parsed);
        }
        return details;
    }

    private static Object first(Map<String, Object> details, String... keys) {
        for (String key : keys)
            if (details.get(key) != null)
                return details.get(key);
        return null;
    }

    private static Integer number(Object value) {
        if (value instanceof Number n)
            return n.intValue();
        if (value instanceof String s && s.matches("-?\\d{1,9}"))
            return Integer.parseInt(s);
        return null;
    }
}
