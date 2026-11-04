package pl.databucket.server.service.data.v2;

import pl.databucket.server.exception.InvalidRuleException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * API version 2: converts the 'rules' of a request into JsonLogic, the format of the rich filter, so both go through
 * the same safe translation ({@link LogicSql}). Rules given as SQL text and the '#func:' prefix are refused.
 *
 * <pre>
 * rules:  [ ["$.city", "==", "Paris"], {"or": [["tagId", "in", [1, 2]], ["$.age", ">", 30]]} ]
 * logic:  {"and": [ {"==": [{"var": "$.city"}, "Paris"]}, {"or": [ {"in": [{"var": "tagId"}, [1, 2]]}, {">": [{"var": "$.age"}, 30]} ]} ]}
 * </pre>
 */
public final class RulesToLogic {

    /** Fields of a data row a rule can refer to by name. */
    static final Set<String> FIELDS = Set.of("id", "tagId", "owner", "reserved", "properties",
            "createdBy", "createdAt", "modifiedBy", "modifiedAt");

    /** Rule operators and their JsonLogic names (see {@link LogicSql}). */
    private static final Map<String, String> OPERATORS = Map.ofEntries(
            Map.entry("=", "=="), Map.entry("==", "=="),
            Map.entry("<>", "!="), Map.entry("!=", "!="),
            Map.entry(">", ">"), Map.entry(">=", ">="), Map.entry("<", "<"), Map.entry("<=", "<="),
            Map.entry("in", "in"), Map.entry("!in", "!in"),
            Map.entry("is", "is"), Map.entry("is not", "is not"),
            Map.entry("like", "like"), Map.entry("!like", "!like"),
            Map.entry("similar", "similar"), Map.entry("!similar", "!similar"),
            Map.entry("~", "~"), Map.entry("!~", "!~"), Map.entry("~*", "~*"), Map.entry("!~*", "!~*"));

    static final String RAW_SQL = "Rules given as SQL text are not supported with the header Databucket-Api-Version: 2."
            + " Write the rule as [left, operator, right], e.g. [\"$.name\", \"like\", \"A%\"].";

    private RulesToLogic() {
    }

    /** The rules as one JsonLogic expression; the items of a list are combined with "and". */
    public static Object convert(List<Object> rules) throws InvalidRuleException {
        return and(rules);
    }

    private static Map<String, Object> and(List<?> items) throws InvalidRuleException {
        List<Object> converted = new ArrayList<>();
        for (Object item : items)
            converted.add(item(item));
        return Map.of("and", converted);
    }

    private static Object item(Object item) throws InvalidRuleException {
        if (item instanceof String)
            throw new InvalidRuleException(RAW_SQL);

        if (item instanceof List<?> rule) {
            if (rule.size() != 3 || !(rule.get(1) instanceof String operatorText))
                throw new InvalidRuleException("A rule needs 3 items [left, operator, right]: " + rule);
            String operator = OPERATORS.get(operatorText.trim().toLowerCase());
            if (operator == null)
                throw new InvalidRuleException("Unknown operator '" + operatorText + "' in the rule " + rule);
            List<Object> args = new ArrayList<>();
            args.add(operand(rule.get(0)));
            args.add(operand(rule.get(2)));
            return Map.of(operator, args);
        }

        if (item instanceof Map<?, ?> map) {
            if (map.size() != 1)
                throw new InvalidRuleException("Expected one operator (and, or, !) in " + map);
            Map.Entry<?, ?> entry = map.entrySet().iterator().next();
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            switch (key) {
                case "and", "or" -> {
                    if (!(value instanceof List<?> list))
                        throw new InvalidRuleException("Expected a list of rules in '" + key + "'");
                    List<Object> converted = new ArrayList<>();
                    for (Object child : list)
                        converted.add(item(child));
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put(key, converted);
                    return result;
                }
                case "!" -> {
                    // {"!": {"and": [...]}} or {"!": [rule, ...]}
                    Object negated = value instanceof List<?> list && !list.isEmpty() && !(list.get(0) instanceof String)
                            && (list.get(0) instanceof List || list.get(0) instanceof Map) ? and(list) : item(value);
                    return Map.of("!", negated);
                }
                default -> throw new InvalidRuleException("Expected one of the operators [and, or, !], got '" + key + "'");
            }
        }

        throw new InvalidRuleException("Expected a rule [left, operator, right] or {and/or/!: ...}, got: " + item);
    }

    /** A field, a property ($.a.b) or a property function ($.a.length()) becomes a variable, anything else a value. */
    private static Object operand(Object value) throws InvalidRuleException {
        if (value instanceof String text) {
            if (text.startsWith("#func:"))
                throw new InvalidRuleException(RAW_SQL);
            if (text.startsWith("$") || FIELDS.contains(text))
                return Map.of("var", text);
        }
        return value;
    }
}
