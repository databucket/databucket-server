package pl.databucket.server.service.data.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.databucket.server.exception.InvalidRuleException;
import pl.databucket.server.service.data.COL;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * API version 2: translates a JsonLogic filter into the WHERE condition of a bucket table.
 * <p>
 * Nothing from the request becomes SQL text: values and the nodes of property paths are parameters, fields are
 * mapped from a fixed list, operators and functions come from fixed lists. Anything else is refused.
 * <p>
 * Operators: and, or, ! (not; with a variable: is empty), !! (is not empty), ==, !=, &lt;, &lt;=, &gt;, &gt;= (also
 * "between": {"&lt;=": [1, {"var": "$.a"}, 5]}), is, is not, in, !in, like, !like, similar, !similar, ~, !~, ~*, !~*.
 * <p>
 * Variables ({"var": ...}): fields (id, tagId, owner, reserved, properties, createdBy, createdAt, modifiedBy,
 * modifiedAt), properties ($.a.b, or prop.$*a*b as the rich filter writes them), property functions
 * ($.a.length(), exists(), notExists(), isNull(), isNotNull(), isEmpty(), isNotEmpty()). "@currentUser" as a value
 * is the signed-in user.
 */
public final class LogicSql {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, String> FIELDS = Map.ofEntries(
            Map.entry("id", COL.DATA_ID), Map.entry("tagId", COL.TAG_ID), Map.entry("owner", COL.RESERVED_BY),
            Map.entry("reserved", COL.RESERVED), Map.entry("properties", COL.PROPERTIES),
            Map.entry("createdBy", COL.CREATED_BY), Map.entry("createdAt", COL.CREATED_AT),
            Map.entry("modifiedBy", COL.MODIFIED_BY), Map.entry("modifiedAt", COL.MODIFIED_AT));

    /** Column names are accepted as well. */
    private static final Set<String> COLUMNS = Set.of(COL.DATA_ID, COL.TAG_ID, COL.RESERVED, COL.RESERVED_BY,
            COL.PROPERTIES, COL.CREATED_BY, COL.CREATED_AT, COL.MODIFIED_BY, COL.MODIFIED_AT);

    private static final Set<String> FUNCTIONS = Set.of("length()", "exists()", "notExists()", "isNull()",
            "isNotNull()", "isEmpty()", "isNotEmpty()");

    private static final Map<String, String> COMPARISONS = Map.of("==", "=", "!=", "<>", "<", "<", "<=", "<=",
            ">", ">", ">=", ">=", "is", "IS NOT DISTINCT FROM", "is not", "IS DISTINCT FROM");

    private static final Map<String, String> PATTERNS = Map.of("like", "LIKE", "!like", "NOT LIKE",
            "similar", "SIMILAR TO", "!similar", "NOT SIMILAR TO", "~", "~", "!~", "!~", "~*", "~*", "!~*", "!~*");

    private final String currentUser;
    private final Map<String, Object> params;
    private final String prefix;
    private int counter;

    private LogicSql(String currentUser, Map<String, Object> params, String prefix) {
        this.currentUser = currentUser;
        this.params = params;
        this.prefix = prefix;
    }

    /**
     * @param prefix names of the parameters this call adds (prefix0, prefix1, ...); the same logic rendered again with
     *               the same prefix gets the same names and values
     * @return the condition, or null for no logic
     */
    public static String toSql(Object logic, String currentUser, Map<String, Object> params, String prefix) throws InvalidRuleException {
        if (logic == null || (logic instanceof Map<?, ?> map && map.isEmpty()))
            return null;
        return new LogicSql(currentUser, params, prefix).node(logic);
    }

    // ---- operands

    private sealed interface Operand permits Column, Property, Function, Value {
    }

    private record Column(String name) implements Operand {
    }

    /** A property; asTimestamp for the old "$.a::timestamp" spelling of the rules. */
    private record Property(String path, boolean asTimestamp) implements Operand {
    }

    private record Function(String path, String name) implements Operand {
    }

    private record Value(Object value) implements Operand {
    }

    private Operand operand(Object item) throws InvalidRuleException {
        if (item instanceof Map<?, ?> map) {
            if (map.size() == 1 && map.containsKey("var"))
                return variable(String.valueOf(map.get("var")));
            throw new InvalidRuleException("Expected a value or {\"var\": ...}, got: " + map);
        }
        if ("@currentUser".equals(item))
            return new Value(currentUser);
        if (item instanceof List<?> list)
            return new Value(list.stream().map(v -> "@currentUser".equals(v) ? currentUser : v).toList());
        return new Value(item);
    }

    private Operand variable(String var) throws InvalidRuleException {
        if (var.startsWith("prop.$*"))
            var = "$." + var.substring("prop.$*".length()).replace('*', '.');

        if (var.startsWith("$.")) {
            if (var.endsWith("()")) {
                int dot = var.lastIndexOf('.');
                String function = var.substring(dot + 1);
                if (dot < 2 || !FUNCTIONS.contains(function))
                    throw new InvalidRuleException("Unknown property function: " + var);
                return new Function(checkPath(var.substring(0, dot)), function);
            }
            if (var.endsWith("::timestamp"))
                return new Property(checkPath(var.substring(0, var.length() - "::timestamp".length())), true);
            return new Property(checkPath(var), false);
        }

        if (FIELDS.containsKey(var))
            return new Column(FIELDS.get(var));
        if (COLUMNS.contains(var))
            return new Column(var);
        throw new InvalidRuleException("Unknown field: " + var);
    }

    private static String checkPath(String path) throws InvalidRuleException {
        String[] nodes = path.substring(2).split("\\.", -1);
        for (String node : nodes)
            if (node.isEmpty())
                throw new InvalidRuleException("Invalid property path: " + path);
        return path;
    }

    // ---- nodes

    private String node(Object node) throws InvalidRuleException {
        if (node instanceof Boolean b)
            return b ? "TRUE" : "FALSE";
        if (!(node instanceof Map<?, ?> map) || map.size() != 1)
            throw new InvalidRuleException("Expected one operator, e.g. {\"==\": [...]}, got: " + node);

        Map.Entry<?, ?> entry = map.entrySet().iterator().next();
        String operator = String.valueOf(entry.getKey());
        List<?> args = entry.getValue() instanceof List<?> list ? list : List.of(entry.getValue());

        switch (operator) {
            case "and", "or" -> {
                if (args.isEmpty())
                    return "and".equals(operator) ? "TRUE" : "FALSE";
                List<String> parts = new ArrayList<>();
                for (Object arg : args)
                    parts.add(node(arg));
                return "(" + String.join(" " + operator.toUpperCase() + " ", parts) + ")";
            }
            case "!" -> {
                Object arg = single(operator, args);
                if (isVariable(arg))
                    return isEmpty(operand(arg), true);
                return "NOT (" + node(arg) + ")";
            }
            case "!!" -> {
                Object arg = single(operator, args);
                if (isVariable(arg))
                    return isEmpty(operand(arg), false);
                return node(arg);
            }
            case "in" -> {
                return in(args);
            }
            case "!in" -> {
                return "NOT (" + in(args) + ")";
            }
            default -> {
                if (COMPARISONS.containsKey(operator))
                    return comparison(operator, args);
                if (PATTERNS.containsKey(operator))
                    return pattern(operator, args);
                throw new InvalidRuleException("Unknown operator: " + operator);
            }
        }
    }

    private static Object single(String operator, List<?> args) throws InvalidRuleException {
        if (args.size() != 1)
            throw new InvalidRuleException("'" + operator + "' takes one argument");
        return args.get(0);
    }

    private static boolean isVariable(Object item) {
        return item instanceof Map<?, ?> map && map.size() == 1 && map.containsKey("var");
    }

    private static List<?> two(String operator, List<?> args) throws InvalidRuleException {
        if (args.size() != 2)
            throw new InvalidRuleException("'" + operator + "' takes two arguments, got " + args.size());
        return args;
    }

    // ---- empty

    private String isEmpty(Operand operand, boolean empty) throws InvalidRuleException {
        String not = empty ? "" : "NOT ";
        if (operand instanceof Column column)
            return "(" + column.name() + " IS " + not + "NULL)";
        if (operand instanceof Property property)
            return "(" + text(property) + " IS " + not + "NULL)";
        throw new InvalidRuleException("'!' and '!!' take a field or a property");
    }

    // ---- comparisons

    private String comparison(String operator, List<?> args) throws InvalidRuleException {
        // between: {"<=": [1, {"var": "$.a"}, 5]}
        if (args.size() == 3 && ("<".equals(operator) || "<=".equals(operator)))
            return "(" + comparison(operator, List.of(args.get(0), args.get(1)))
                    + " AND " + comparison(operator, List.of(args.get(1), args.get(2))) + ")";

        two(operator, args);
        Operand left = operand(args.get(0));
        Operand right = operand(args.get(1));

        // a value on the left: swap the sides
        if (left instanceof Value && !(right instanceof Value)) {
            Operand tmp = left;
            left = right;
            right = tmp;
            operator = switch (operator) {
                case "<" -> ">";
                case ">" -> "<";
                case "<=" -> ">=";
                case ">=" -> "<=";
                default -> operator;
            };
        }
        if (left instanceof Value)
            throw new InvalidRuleException("Both sides of the rule are values; a property needs the '$.' prefix: " + args);

        String op = COMPARISONS.get(operator);

        if (right instanceof Value value) {
            Object v = value.value();
            if (v == null) {
                boolean equal = "==".equals(operator) || "is".equals(operator);
                boolean notEqual = "!=".equals(operator) || "is not".equals(operator);
                if (!equal && !notEqual)
                    throw new InvalidRuleException("'" + operator + "' can't compare with null");
                String expr = left instanceof Property p ? text(p) : expression(left);
                return "(" + expr + (equal ? " IS NULL)" : " IS NOT NULL)");
            }
            if (v instanceof Collection || v instanceof Map)
                throw new InvalidRuleException("'" + operator + "' compares with a single value, got: " + v);
            return "(" + typedLeft(left, v) + " " + op + " " + param(typedValue(left, v)) + ")";
        }

        // two variables
        if (left instanceof Property l && right instanceof Property r)
            return "(" + json(l) + " " + op + " " + json(r) + ")";
        return "(" + textOf(left) + " " + op + " " + textOf(right) + ")";
    }

    /** The left side, cast to the type of the value it is compared with (as API version 1 did). */
    private String typedLeft(Operand left, Object value) throws InvalidRuleException {
        if (left instanceof Property property) {
            if (value instanceof Boolean) {
                String json = json(property);
                return "(CASE WHEN jsonb_typeof(" + json + ") = 'boolean' THEN (" + json + ")::boolean END)";
            }
            if (value instanceof Number) {
                String json = json(property);
                return "(CASE WHEN jsonb_typeof(" + json + ") = 'number' THEN (" + json + ")::numeric END)";
            }
            if (property.asTimestamp() || date(value) != null) {
                String text = text(property);
                return "(CASE WHEN " + text + " ~ '^\\d{4}-\\d{2}-\\d{2}' THEN (" + text + ")::timestamp END)";
            }
            return text(property);
        }
        return expression(left);
    }

    private static Object typedValue(Operand left, Object value) {
        if (value instanceof String text && (left instanceof Column || left instanceof Property)) {
            Timestamp timestamp = date(text);
            if (timestamp != null)
                return timestamp;
        }
        if (value instanceof Number number && left instanceof Property)
            return new java.math.BigDecimal(number.toString());
        return value;
    }

    private static Timestamp date(Object value) {
        if (!(value instanceof String text))
            return null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        format.setLenient(false);
        try {
            return new Timestamp(format.parse(text.trim()).getTime());
        } catch (ParseException e) {
            return null;
        }
    }

    // ---- patterns

    private String pattern(String operator, List<?> args) throws InvalidRuleException {
        two(operator, args);
        Operand left = operand(args.get(0));
        Operand right = operand(args.get(1));
        if (left instanceof Value)
            throw new InvalidRuleException("'" + operator + "' needs a field or a property on the left: " + args);
        String rightSql = right instanceof Value value ? param(value.value() == null ? null : String.valueOf(value.value())) : textOf(right);
        return "(" + textOf(left) + " " + PATTERNS.get(operator) + " " + rightSql + ")";
    }

    // ---- in

    private String in(List<?> args) throws InvalidRuleException {
        two("in", args);
        Operand left = operand(args.get(0));
        Operand right = operand(args.get(1));

        // {"in": [{"var": "tagId"}, [1, 2]]}: the field or property is one of the values
        if (!(left instanceof Value) && right instanceof Value values) {
            List<?> list = values.value() instanceof Collection<?> c ? List.copyOf(c) : java.util.Collections.singletonList(values.value());
            if (list.isEmpty())
                return "FALSE";
            if (left instanceof Column column)
                return "(" + column.name() + " IN (" + param(list) + "))";
            if (left instanceof Property property)
                return "(" + jsonParam(list) + " @> " + json(property) + ")";
            throw new InvalidRuleException("'in' doesn't take a property function on the left");
        }

        // {"in": ["abc", {"var": "$.text"}]}: the value is in the property - an item of a list, or a part of a text
        if (left instanceof Value value && right instanceof Property property) {
            String json = json(property);
            String contains = "(jsonb_typeof(" + json + ") = 'array' AND " + json + " @> " + jsonParam(List.of(value.value())) + ")";
            if (value.value() instanceof String text)
                return "(" + contains + " OR (jsonb_typeof(" + json + ") = 'string' AND " + text(property) + " LIKE " + param(likeContains(text)) + "))";
            return contains;
        }
        if (left instanceof Value value && right instanceof Column column && value.value() instanceof String text)
            return "((" + column.name() + ")::varchar LIKE " + param(likeContains(text)) + ")";

        throw new InvalidRuleException("Unsupported 'in': " + args);
    }

    private static String likeContains(String text) {
        return "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    // ---- SQL pieces

    private String path(String path) {
        List<String> elements = new ArrayList<>();
        for (String node : path.substring(2).split("\\."))
            elements.add("CAST(" + param(node) + " AS text)");
        return "ARRAY[" + String.join(", ", elements) + "]";
    }

    private String json(Property property) {
        return "(properties #> " + path(property.path()) + ")";
    }

    private String text(Property property) {
        return "(properties #>> " + path(property.path()) + ")";
    }

    private String expression(Operand operand) throws InvalidRuleException {
        if (operand instanceof Column column)
            return column.name();
        if (operand instanceof Property property)
            return text(property);
        if (operand instanceof Function function) {
            Property property = new Property(function.path(), false);
            return switch (function.name()) {
                case "length()" -> "COALESCE(jsonb_array_length(CASE WHEN jsonb_typeof(" + json(property) + ") = 'array' THEN "
                        + json(property) + " END), 0)";
                case "exists()" -> "(" + json(property) + " IS NOT NULL)";
                case "notExists()" -> "(" + json(property) + " IS NULL)";
                case "isNotNull()", "isNotEmpty()" -> "(" + text(property) + " IS NOT NULL)";
                case "isNull()", "isEmpty()" -> "(" + text(property) + " IS NULL)";
                default -> throw new InvalidRuleException("Unknown property function: " + function.name());
            };
        }
        throw new InvalidRuleException("Expected a field or a property");
    }

    /** Any operand as text, for patterns and comparisons of two variables. */
    private String textOf(Operand operand) throws InvalidRuleException {
        if (operand instanceof Value value)
            return param(value.value());
        if (operand instanceof Property property)
            return text(property);
        return "(" + expression(operand) + ")::varchar";
    }

    private String jsonParam(List<?> values) {
        return "CAST(" + param(mapper.valueToTree(values).toString()) + " AS jsonb)";
    }

    private String param(Object value) {
        String name = prefix + counter++;
        params.put(name, value);
        return ":" + name;
    }
}
