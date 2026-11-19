package pl.databucket.server.service.data.filter;

import pl.databucket.server.exception.InvalidRuleException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts the 'conditions' of a request (the oldest filter format) into JsonLogic, so they go through the same safe
 * translation as the rules ({@link LogicSql}). The conditions are combined with "and".
 *
 * <pre>
 * conditions: [{"left_source": "property", "left_value": "$.city", "operator": "=", "right_source": "const", "right_value": "Paris"}]
 * logic:      {"and": [{"==": [{"var": "$.city"}, "Paris"]}]}
 * </pre>
 * <p>
 * Sources: const (a value), field (id, tagId, owner, reserved, properties, createdBy, createdAt, modifiedBy,
 * modifiedAt or their column names), property ($.a.b) and function (a property function, e.g. $.a.length()).
 * SQL text (a function without '$.') is refused.
 */
public final class ConditionsToLogic {

    static final String LEFT_SOURCE = "left_source";
    static final String LEFT_VALUE = "left_value";
    static final String OPERATOR = "operator";
    static final String RIGHT_SOURCE = "right_source";
    static final String RIGHT_VALUE = "right_value";

    /** Condition operators and their JsonLogic names (see {@link LogicSql}). */
    private static final Map<String, String> OPERATORS = Map.ofEntries(
            Map.entry("=", "=="), Map.entry("==", "=="),
            Map.entry("<>", "!="), Map.entry("!=", "!="),
            Map.entry(">", ">"), Map.entry(">=", ">="), Map.entry("<", "<"), Map.entry("<=", "<="),
            Map.entry("in", "in"), Map.entry("!in", "!in"),
            Map.entry("is", "is"), Map.entry("is not", "is not"),
            Map.entry("like", "like"), Map.entry("!like", "!like"),
            Map.entry("similar", "similar"), Map.entry("!similar", "!similar"),
            Map.entry("~", "~"), Map.entry("!~", "!~"), Map.entry("~*", "~*"), Map.entry("!~*", "!~*"));

    private ConditionsToLogic() {
    }

    /** The conditions as one JsonLogic expression. */
    public static Object convert(List<Map<String, Object>> conditions) throws InvalidRuleException {
        List<Object> converted = new ArrayList<>();
        for (Map<String, Object> condition : conditions)
            converted.add(condition(condition));
        return Map.of("and", converted);
    }

    private static Object condition(Map<String, Object> condition) throws InvalidRuleException {
        if (condition == null)
            throw new InvalidRuleException("A condition can't be null");
        if (!(condition.get(OPERATOR) instanceof String operatorText))
            throw new InvalidRuleException("A condition needs an operator: " + condition);
        String operator = OPERATORS.get(operatorText.trim().toLowerCase());
        if (operator == null)
            throw new InvalidRuleException("Unknown operator '" + operatorText + "' in the condition " + condition);

        Object left = operand(condition, LEFT_SOURCE, LEFT_VALUE);
        Object right = operand(condition, RIGHT_SOURCE, RIGHT_VALUE);
        if (("in".equals(operator) || "!in".equals(operator)) && "const".equals(source(condition, RIGHT_SOURCE)))
            right = list(right);
        return Map.of(operator, java.util.Arrays.asList(left, right));
    }

    private static String source(Map<String, Object> condition, String key) {
        Object source = condition.get(key);
        return source == null ? null : String.valueOf(source).trim().toLowerCase();
    }

    private static Object operand(Map<String, Object> condition, String sourceKey, String valueKey) throws InvalidRuleException {
        String source = source(condition, sourceKey);
        Object value = condition.get(valueKey);
        if (source == null)
            throw new InvalidRuleException("A condition needs '" + sourceKey + "' (const, field, property or function): " + condition);
        switch (source) {
            case "const" -> {
                return value;
            }
            case "field" -> {
                if (!(value instanceof String field) || field.isBlank())
                    throw new InvalidRuleException("Expected a field name in '" + valueKey + "': " + condition);
                return Map.of("var", field.trim());
            }
            case "property", "function" -> {
                if (!(value instanceof String path) || !path.trim().startsWith("$."))
                    throw new InvalidRuleException("Expected a property starting with '$.' in '" + valueKey + "' (SQL text is not "
                            + "supported): " + condition);
                return Map.of("var", path.trim());
            }
            default -> throw new InvalidRuleException("Unknown '" + sourceKey + "': " + source + " (use const, field, property or function)");
        }
    }

    /** The values of 'in': a list, or a text "a, b" / "[1, 2]" as the old conditions accepted it. */
    private static Object list(Object value) {
        if (!(value instanceof String text) || !text.contains(","))
            return value;
        String trimmed = text.trim();
        if (trimmed.startsWith("["))
            trimmed = trimmed.substring(1);
        if (trimmed.endsWith("]"))
            trimmed = trimmed.substring(0, trimmed.length() - 1);

        List<String> items = new ArrayList<>();
        for (String item : trimmed.split(",")) {
            item = item.trim();
            if (item.startsWith("\""))
                item = item.substring(1);
            if (item.endsWith("\""))
                item = item.substring(0, item.length() - 1);
            items.add(item);
        }

        try {
            List<Object> numbers = new ArrayList<>();
            for (String item : items)
                numbers.add(item.matches("-?\\d+") ? (Object) Long.parseLong(item) : new java.math.BigDecimal(item));
            return numbers;
        } catch (NumberFormatException e) {
            return new ArrayList<Object>(items);
        }
    }
}
