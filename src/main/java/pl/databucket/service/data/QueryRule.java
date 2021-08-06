package pl.databucket.service.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.InvalidObjectException;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
public class QueryRule {

    private Operator operator = Operator.and;
    private List<Condition> conditions = new ArrayList<>();
    private List<String> strRules = new ArrayList<>(); // given in the rules as string
    private List<QueryRule> queryRules = new ArrayList<>();
    private String currentUser;

    public QueryRule(String currentUser, SearchRules searchRules) throws InvalidObjectException {
        this.currentUser = currentUser;

        if (searchRules.getLogic() != null)
            getQueryRulesFromLogic(currentUser, queryRules, conditions, searchRules.getLogic());

        if (searchRules.getRules() != null)
            getQueryRulesFromRules(currentUser, queryRules, conditions, strRules, searchRules.getRules());

        if (searchRules.getConditions() != null) {
            for (Map<String, Object> conditionMap : searchRules.getConditions())
                conditions.add(new Condition(conditionMap));
        }
    }

    private void getQueryRulesFromLogic(String localCurrentUser, List<QueryRule> localQueryRule, List<Condition> localConditions, Object inputLogic) {
        this.currentUser = localCurrentUser;
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
//                        queryRule.setQueryRules(new ArrayList<>());
                        localQueryRule.add(queryRule);
                        getQueryRulesFromLogic(localCurrentUser, queryRule.getQueryRules(), queryRule.getConditions(), value);
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
//                            queryRule.setQueryRules(new ArrayList<>());
                            localQueryRule.add(queryRule);
                            getQueryRulesFromLogic(localCurrentUser, queryRule.getQueryRules(), queryRule.getConditions(), value);
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
            ((List<Object>) inputLogic).forEach(item -> getQueryRulesFromLogic(localCurrentUser, localQueryRule, localConditions, item));
    }

    private void getQueryRulesFromRules(String localCurrentUser, List<QueryRule> localQueryRule, List<Condition> localConditions, List<String> localStrRules, Object inputRules) throws InvalidObjectException {
        this.currentUser = localCurrentUser;
        if (inputRules instanceof ArrayList)
            for (Object inputRuleItem : (List<Object>) inputRules) {
                // nested rules with one item witch is an operator ["and", "or", "!"]
                if (inputRuleItem instanceof Map) {
                    Map<String, Object> inputRuleItemMap = (Map<String, Object>) inputRuleItem;
                    if (inputRuleItemMap.size() == 1) {
                        inputRuleItemMap.forEach((key, value) -> {
                            QueryRule queryRule;
                            Operator operator = Operator.fromString(key);
                            switch (Objects.requireNonNull(operator)) {
                                case and:
                                case or:
                                    queryRule = new QueryRule();
                                    queryRule.setOperator(operator);
                                    queryRule.setQueryRules(new ArrayList<>());
                                    localQueryRule.add(queryRule);

                                    // Value should be an array
                                    if (!(value instanceof ArrayList))
                                        throw new IllegalStateException("Expected array value in the '" + key + "' item");

                                    try {
                                        getQueryRulesFromRules(localCurrentUser, queryRule.getQueryRules(), queryRule.getConditions(), queryRule.getStrRules(), value);
                                    } catch (InvalidObjectException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                case not:
                                    // e.g. !{"and": ...}
                                    queryRule = new QueryRule();
                                    queryRule.setOperator(operator);
                                    localQueryRule.add(queryRule);
                                    try {
                                        getQueryRulesFromRules(localCurrentUser, queryRule.getQueryRules(), queryRule.getConditions(), queryRule.getStrRules(), value);
                                    } catch (InvalidObjectException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                default:
                                    throw new IllegalStateException("Expected one of the operators [and, or, !]");
                            }
                        });
                    } else
                        throw new InvalidObjectException("Expected 1 item as logic operator");

                    // new rule as array ["leftFieldOrItem", "operator", "rightFieldOrItem"] e.g.: ["$.name", "like", "Jonh%"]
                } else if (inputRuleItem instanceof ArrayList) {
                    List<Object> ruleItem = (List<Object>) inputRuleItem;
                    if (ruleItem.size() == 3)
                        localConditions.add(getConditionFromRule(ruleItem.get(0), Operator.fromString((String) ruleItem.get(1)), ruleItem.get(2)));
                    else
                        throw new InvalidObjectException("Expected 3 items in the rule definition!");

                    // whole sql condition as string, e.g.: (properties->'account' is not NULL) = true
                } else if (inputRuleItem instanceof String) {
                    localStrRules.add(retrieveDatabaseColumnsNames((String) inputRuleItem));
                } else
                    throw new InvalidObjectException("Expected Map or ArrayList in as the 'rule' item!");
            }

        // inside ! operator
        else if (inputRules instanceof Map) {
            Map<String, Object> inputRuleItemMap = (Map<String, Object>) inputRules;
            if (inputRuleItemMap.size() == 1) {
                inputRuleItemMap.forEach((key, value) -> {
                    QueryRule queryRule;
                    Operator operator = Operator.fromString(key);
                    switch (Objects.requireNonNull(operator)) {
                        case and:
                        case or:
                            queryRule = new QueryRule();
                            queryRule.setOperator(operator);
                            queryRule.setQueryRules(new ArrayList<>());
                            localQueryRule.add(queryRule);

                            // Value should be an array
                            if (!(value instanceof ArrayList))
                                throw new IllegalStateException("Expected array value in the '" + key + "' item");

                            try {
                                getQueryRulesFromRules(localCurrentUser, queryRule.getQueryRules(), queryRule.getConditions(), queryRule.getStrRules(), value);
                            } catch (InvalidObjectException e) {
                                e.printStackTrace();
                            }
                            break;
                        case not:
                            // e.g. !{"and": ...}
                            queryRule = new QueryRule();
                            queryRule.setOperator(operator);
                            localQueryRule.add(queryRule);
                            try {
                                getQueryRulesFromRules(localCurrentUser, queryRule.getQueryRules(), queryRule.getConditions(), queryRule.getStrRules(), value);
                            } catch (InvalidObjectException e) {
                                e.printStackTrace();
                            }
                            break;
                        default:
                            throw new IllegalStateException("Expected one of the operators [and, or, !]");
                    }
                });
            } else
                throw new InvalidObjectException("Expected 1 item as logic operator");
        }
    }

    private Condition getConditionFromRule(Object leftObject, Operator operator, Object rightObject) {
        String leftValue = (String) leftObject;
        SourceType leftSource = leftValue.startsWith("$") ? leftValue.endsWith("()") ? SourceType.s_function : SourceType.s_property : SourceType.s_field;
        Object rightValue = retrieveCurrentUser(rightObject);
        SourceType rightSource = SourceType.s_const;

        return new Condition(leftSource, getDatabaseColumnName(leftValue), operator, rightSource, rightValue);
    }

    private Condition getConditionFromJsonLogic(Operator operator, Object firstItem, Object secondItem) {
        switch (operator) {
            case equal:
            case notEqual:
                return new Condition(convertJsonLogicVariable(firstItem), operator, retrieveCurrentUser(secondItem));
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
                    return new Condition(convertJsonLogicVariable(firstItem), operator, retrieveCurrentUser(secondItem));
            default:
                throw new UnsupportedOperationException("");
        }
    }

    Object retrieveCurrentUser(Object obj) {
        if (obj.equals("@currentUser"))
            return currentUser;
        else if (obj instanceof List)
            Collections.replaceAll((ArrayList) obj, "@currentUser", currentUser);

        return obj;
    }

    private String convertJsonLogicVariable(Object variable) {
        String var = ((Map<String, String>) variable).get("var");
        if (var.startsWith("prop.$*"))
            var = var.replace("prop.", "").replace("*", ".");
        else
            var = getDatabaseColumnName(var);
        return var;
    }

    private String retrieveDatabaseColumnsNames(String rule) {
        return rule
                .replace("id", COL.DATA_ID)
                .replace("tagId", COL.TAG_ID)
                .replace("owner", COL.RESERVED_BY)
                .replace("createdBy", COL.CREATED_BY)
                .replace("createdAt", COL.CREATED_AT)
                .replace("modifiedBy", COL.MODIFIED_BY)
                .replace("modifiedAt", COL.MODIFIED_AT)
                .replace("@currentUser", this.currentUser);
    }

    private String getDatabaseColumnName(String field) {
        switch (field) {
            case "id":
                return COL.DATA_ID;
            case "tagId":
                return COL.TAG_ID;
            case "owner":
                return COL.RESERVED_BY;
            case "createdBy":
                return COL.CREATED_BY;
            case "createdAt":
                return COL.CREATED_AT;
            case "modifiedBy":
                return COL.MODIFIED_BY;
            case "modifiedAt":
                return COL.MODIFIED_AT;
            default:
                return field;
        }
    }
}
