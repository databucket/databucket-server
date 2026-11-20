package pl.databucket.server.mcp;

import pl.databucket.server.dto.IndexedPropertyDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The fields of a new bucket's data class, from an example row (types guessed from the values) and/or a list of
 * field definitions (they win over the example, by path). Checked here, before anything is created.
 */
final class BucketSchema {

    static final int MAX_FIELDS = 100;
    static final int MAX_ENUM_VALUES = 200;
    static final String ENUM = "select";
    /** The types of the data class (as the GUI knows them); "enum" is taken for "select". */
    static final Set<String> TYPES = Set.of("string", "numeric", "boolean", "date", "datetime", "time", ENUM);
    private static final Pattern DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern DATETIME = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}(:\\d{2}(\\.\\d+)?)?(Z|[+-]\\d{2}:?\\d{2})?");
    private static final Pattern TIME = Pattern.compile("\\d{2}:\\d{2}(:\\d{2})?");

    record Field(String path, String title, String type, List<String> values) {
    }

    record Result(List<Field> fields, List<String> skipped) {
    }

    private BucketSchema() {
    }

    /**
     * @param sample      an example row (its properties), may be null
     * @param definitions field definitions {path, title?, type?, values?}, may be null
     */
    static Result of(Map<String, Object> sample, List<Map<String, Object>> definitions) {
        Map<String, Field> fields = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();
        if (sample != null)
            fromSample("$", sample, fields, skipped);
        if (definitions != null)
            for (Map<String, Object> definition : definitions)
                define(definition, fields);
        if (fields.isEmpty())
            throw new IllegalArgumentException("Give the fields of the data: an example row in 'sample' and/or a list "
                    + "in 'fields', e.g. [{\"path\": \"$.status\", \"type\": \"enum\", \"values\": [\"active\", \"blocked\"]}]");
        if (fields.size() > MAX_FIELDS)
            throw new IllegalArgumentException("At most " + MAX_FIELDS + " fields");

        List<String> paths = new ArrayList<>(fields.keySet());
        for (String a : paths)
            for (String b : paths)
                if (b.startsWith(a + "."))
                    throw new IllegalArgumentException("The fields '" + a + "' and '" + b + "' overlap: '" + a
                            + "' would be an object, give its inner fields only");
        return new Result(List.copyOf(fields.values()), skipped);
    }

    // ---------------------------------------------------------------- example row

    @SuppressWarnings("unchecked")
    private static void fromSample(String prefix, Map<String, Object> object, Map<String, Field> fields, List<String> skipped) {
        object.forEach((key, value) -> {
            String path = prefix + "." + key;
            if (key.contains("."))
                throw new IllegalArgumentException("'" + key + "' in the sample is not a usable property path: a key "
                        + "can't have a dot; nest objects instead");
            if (value instanceof Map<?, ?> inner && !inner.isEmpty())
                fromSample(path, (Map<String, Object>) inner, fields, skipped);
            else if (value instanceof List<?> || value instanceof Map<?, ?>)
                skipped.add(path + " (a list or an empty object: no column; the rows can still have it)");
            else {
                checkPath(path);
                fields.put(path, new Field(path, title(path), guessType(value), null));
            }
        });
    }

    static String guessType(Object value) {
        if (value instanceof Number)
            return "numeric";
        if (value instanceof Boolean)
            return "boolean";
        if (value instanceof String text) {
            if (DATE.matcher(text).matches())
                return "date";
            if (DATETIME.matcher(text).matches())
                return "datetime";
            if (TIME.matcher(text).matches())
                return "time";
        }
        return "string";
    }

    // ---------------------------------------------------------------- definitions

    private static void define(Map<String, Object> definition, Map<String, Field> fields) {
        if (definition == null || !(definition.get("path") instanceof String rawPath) || rawPath.isBlank())
            throw new IllegalArgumentException("Each field needs a path, e.g. {\"path\": \"$.address.city\"}");
        for (String key : definition.keySet())
            if (!Set.of("path", "title", "type", "values").contains(key))
                throw new IllegalArgumentException("Field '" + rawPath + "': unknown item '" + key
                        + "'; a field has path, title, type and values");
        String path = rawPath.trim().startsWith("$.") ? rawPath.trim() : "$." + rawPath.trim();
        checkPath(path);
        Field before = fields.get(path);

        String title = definition.get("title") instanceof String t && !t.isBlank() ? t.trim()
                : before != null ? before.title() : title(path);
        List<String> values = values(path, definition.get("values"));
        String type = definition.get("type") != null ? type(path, definition.get("type"))
                : values != null ? ENUM : before != null ? before.type() : "string";
        if (ENUM.equals(type) && values == null)
            throw new IllegalArgumentException("Field '" + path + "': an enum needs its values, e.g. "
                    + "\"values\": [\"active\", \"blocked\"]");
        if (!ENUM.equals(type) && values != null)
            throw new IllegalArgumentException("Field '" + path + "': only an enum has values");
        fields.put(path, new Field(path, title, type, values));
    }

    private static String type(String path, Object value) {
        String type = String.valueOf(value).trim().toLowerCase();
        type = switch (type) {
            case "enum" -> ENUM;
            case "number", "integer", "decimal" -> "numeric";
            case "text" -> "string";
            default -> type;
        };
        if (!TYPES.contains(type))
            throw new IllegalArgumentException("Field '" + path + "': the type is one of string, numeric, boolean, date, "
                    + "datetime, time, enum");
        return type;
    }

    private static List<String> values(String path, Object value) {
        if (value == null)
            return null;
        if (!(value instanceof List<?> list) || list.isEmpty() || list.size() > MAX_ENUM_VALUES)
            throw new IllegalArgumentException("Field '" + path + "': values is a list of 1.." + MAX_ENUM_VALUES + " texts");
        List<String> values = new ArrayList<>();
        for (Object item : list) {
            if (item == null || item instanceof Map || item instanceof List)
                throw new IllegalArgumentException("Field '" + path + "': the values are texts or numbers");
            String text = String.valueOf(item);
            if (!values.contains(text))
                values.add(text);
        }
        return values;
    }

    private static void checkPath(String path) {
        if (!path.matches(IndexedPropertyDto.PATH_PATTERN) || path.length() > 100)
            throw new IllegalArgumentException("'" + path + "' is not a usable property path: $.a.b with letters, digits, "
                    + "_, - and spaces");
    }

    /** $.address.postCode -> "Post code", $.first_name -> "First name". */
    static String title(String path) {
        String node = path.substring(path.lastIndexOf('.') + 1);
        String words = node.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replaceAll("[_-]+", " ").trim().toLowerCase();
        return words.isEmpty() ? node : Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
