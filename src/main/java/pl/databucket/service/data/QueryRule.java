package pl.databucket.service.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class QueryRule {

    private Operator operator = Operator.and;
    private List<Condition> conditions = new ArrayList<>();
    private List<QueryRule> queryRules = new ArrayList<>();

    public QueryRule(SearchRules searchRules) {
        if (searchRules.getLogic() != null)
            getQueryRulesFromLogic(queryRules, conditions, searchRules.getLogic());

        if (searchRules.getRules() != null)
            getQueryRulesFromRules(queryRules, searchRules.getRules());

        if (searchRules.getConditions() != null) {
            for (Map<String, Object> conditionMap : searchRules.getConditions())
                conditions.add(new Condition(conditionMap));
        }
    }

    private void getQueryRulesFromLogic(List<QueryRule> localQueryRule, List<Condition> localConditions, Object inputLogic) {
        if (inputLogic instanceof Map) {
            Map<String, Object> inputLogicMap = (Map<String, Object>) inputLogic;
            inputLogicMap.forEach((key, value) -> {
                QueryRule queryRule;
                Operator operator = Operator.fromString(key);
                switch (operator) {
                    case and:
                    case or:
                        queryRule = new QueryRule();
                        queryRule.setOperator(Operator.fromString(key));
                        queryRule.setQueryRules(new ArrayList<>());
                        localQueryRule.add(queryRule);
                        getQueryRulesFromLogic(queryRule.getQueryRules(), queryRule.getConditions(), value);
                        break;
                    case not: // not also means isEmpty in jsonLogic
                    case isNotEmpty:
                        Map<String, Object> child = (Map<String, Object>) value;
                        Map.Entry<String, Object> entry = child.entrySet().iterator().next();
                        if (entry.getKey().equals("var")) {
                            // e.g. !{"var": "$.item"} or !!{"var": "$.item"}
                            String field = convertJsonLogicVariable(value) + ".isEmpty()";
                            localConditions.add(new Condition(field, Operator.equal, operator.equals(Operator.not))); // isEmpty = true or isEmpty = false
                        } else {
                            // e.g. !{"and": [...]}
                            queryRule = new QueryRule();
                            queryRule.setOperator(Operator.fromString(key));
                            queryRule.setQueryRules(new ArrayList<>());
                            localQueryRule.add(queryRule);
                            getQueryRulesFromLogic(queryRule.getQueryRules(), queryRule.getConditions(), value);
                        }
                        break;
                    default:
                        List<Object> items = (List<Object>) value;
                        if (items.size() == 2)
                            localConditions.add(getConditionFromJsonLogic(operator, items.get(0), items.get(1)));
                        else if (items.size() == 3) {  // between
                            String field = convertJsonLogicVariable(items.get(1));
                            Operator invertedOperator = Operator.getInverted(operator);
                            localConditions.add(new Condition(field, invertedOperator, items.get(0)));
                            localConditions.add(new Condition(field, operator, items.get(2)));
                        } else
                            throw new UnsupportedOperationException("Unexpected number of arguments (" + items.size() + ") for this operator " + operator.name());
                }
            });
        } else if (inputLogic instanceof List)
            ((List<Object>) inputLogic).forEach(item -> getQueryRulesFromLogic(localQueryRule, localConditions, item));
    }

    private void getQueryRulesFromRules(List<QueryRule> queryRules, Map<String, Object> inputRules) {
        // TODO: almost the same as in JsonLogic case
    }

    private Condition getConditionFromJsonLogic(Operator operator, Object firstItem, Object secondItem) {
        switch (operator) {
            case equal:
            case notEqual:
            case grater:
            case graterEqual:
            case less:
            case lessEqual:
                return new Condition(convertJsonLogicVariable(firstItem), operator, secondItem);
            case in:
                // "in" as "like" operator
                if (firstItem instanceof String) {
                    return new Condition(convertJsonLogicVariable(secondItem), Operator.like, "%" + firstItem + "%");
                } else
                    return new Condition(convertJsonLogicVariable(firstItem), operator, secondItem);
            default:
                throw new UnsupportedOperationException("");
        }
    }

    private String convertJsonLogicVariable(Object variable) {
        String var = ((Map<String, String>)variable).get("var");
        if (var.startsWith("prop.$*"))
            var = var.replace("prop.", "").replace("*", ".");
        else
            var = getDatabaseColumnName(var);
        return var;
    }

    private String getDatabaseColumnName(String field) {
        switch (field) {
            case "id": return COL.DATA_ID;
            case "tagId": return COL.TAG_ID;
            case "owner": return COL.RESERVED_BY;
            case "createdBy": return COL.CREATED_BY;
            case "createdAt": return COL.CREATED_AT;
            case "modifiedBy": return COL.MODIFIED_BY;
            case "modifiedAt": return COL.MODIFIED_AT;
            default:
                return field;
        }
    }
}
