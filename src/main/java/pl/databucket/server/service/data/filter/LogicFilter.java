package pl.databucket.server.service.data.filter;

import pl.databucket.server.exception.InvalidRuleException;
import pl.databucket.server.service.data.Operator;
import pl.databucket.server.service.data.Query;
import pl.databucket.server.service.data.SearchRules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The filter of a data request: which rows of a bucket table an operation reads, reserves, changes or removes. The
 * 'logic' (JsonLogic), the 'rules' and the 'conditions' of the request (both converted to JsonLogic) are combined
 * with "and" and translated by {@link LogicSql}. Rules given as SQL text are refused.
 */
public final class LogicFilter {

    private final String currentUser;
    private final List<Object> logic = new ArrayList<>();
    private final List<Object[]> required = new ArrayList<>();

    private LogicFilter(String currentUser) {
        this.currentUser = currentUser;
    }

    public static LogicFilter of(String currentUser, SearchRules searchRules) throws InvalidRuleException {
        LogicFilter filter = new LogicFilter(currentUser);
        if (searchRules.getLogic() != null && !searchRules.getLogic().isEmpty())
            filter.logic.add(searchRules.getLogic());
        if (searchRules.getRules() != null && !searchRules.getRules().isEmpty())
            filter.logic.add(RulesToLogic.convert(searchRules.getRules()));
        if (searchRules.getConditions() != null && !searchRules.getConditions().isEmpty())
            filter.logic.add(ConditionsToLogic.convert(searchRules.getConditions()));
        // fail now, not when the query is built
        filter.where(new java.util.HashMap<>());
        return filter;
    }

    /** Whether the request has no logic, rules or conditions (modifying and removing by rules need one). */
    public boolean isEmpty() {
        return logic.isEmpty();
    }

    /** A condition the server adds on a column of the table (e.g. the rows of the user), combined with AND. */
    public void require(String column, Operator operator, Object value) {
        required.add(new Object[]{column, operator, value});
    }

    /** Adds the WHERE clause to the query; its values go into the parameters. */
    public Query applyTo(Query query, Map<String, Object> paramMap) {
        try {
            return query.whereSql(where(paramMap));
        } catch (InvalidRuleException e) {
            // checked in of(); the server's own conditions are always valid
            throw new IllegalStateException(e);
        }
    }

    private String where(Map<String, Object> paramMap) throws InvalidRuleException {
        List<String> parts = new ArrayList<>();
        String sql = LogicSql.toSql(logic.isEmpty() ? null : Map.of("and", logic), currentUser, paramMap, "lg");
        if (sql != null)
            parts.add(sql);
        for (int i = 0; i < required.size(); i++)
            parts.add(requiredSql(i, paramMap));
        return parts.isEmpty() ? null : String.join(" AND ", parts);
    }

    // the server's conditions: a column constant of the code, an operator and a value as a parameter
    private String requiredSql(int i, Map<String, Object> paramMap) {
        String column = (String) required.get(i)[0];
        Operator operator = (Operator) required.get(i)[1];
        Object value = required.get(i)[2];
        String name = "rq" + i;
        if (value == null)
            return "(" + column + (operator == Operator.notEqual ? " IS NOT NULL)" : " IS NULL)");
        paramMap.put(name, value);
        return switch (operator) {
            case equal -> "(" + column + " = :" + name + ")";
            case notEqual -> "(" + column + " IS DISTINCT FROM :" + name + ")";
            case in -> value instanceof Collection<?> c && c.isEmpty() ? "FALSE" : "(" + column + " IN (:" + name + "))";
            default -> throw new IllegalArgumentException("Unsupported operator " + operator);
        };
    }
}
