package pl.databucket.server.service.data;

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

    private boolean isField(String value) {
        String[] fields = {"id", "tagId", "owner", "reserved", "properties", "createdBy", "createdAt", "modifiedBy", "modifiedAt"};
        return Arrays.asList(fields).contains(value);
    }

    private SourceType getSourceType(Object objectValue) {
        if (objectValue instanceof String) {
            String stringValue = (String) objectValue;
            if (stringValue.startsWith("$"))
                if (stringValue.endsWith("()"))
                    return SourceType.s_function;
                else
                    return SourceType.s_property;
            else if (isField(stringValue))
                return SourceType.s_field;
            else
                return SourceType.s_const;
        } else
            return SourceType.s_const;
    }

    private Condition getConditionFromRule(Object leftObject, Operator operator, Object rightObject) {
        SourceType leftSourceType = getSourceType(leftObject);
        SourceType rightSourceType = getSourceType(rightObject);

        leftObject = retrieveCurrentUser(leftObject);
        if (leftObject instanceof String) {
            String leftValue = (String) leftObject;
            if (leftValue.startsWith("#func:")) {
                leftObject = ((String) leftObject).substring(6);
                leftSourceType = SourceType.s_function;
            } else if (leftSourceType.equals(SourceType.s_field))
                leftObject = getDatabaseColumnName(leftValue);
        }

        rightObject = retrieveCurrentUser(rightObject);
        if (rightObject instanceof String) {
            String rightValue = (String) rightObject;
            if (rightValue.startsWith("#func:")) {
                rightObject = ((String) rightObject).substring(6);
                rightSourceType = SourceType.s_function;
            } else if (rightSourceType.equals(SourceType.s_field))
                rightObject = getDatabaseColumnName(rightValue);
        }

        // replace left and right in some situations
        if (leftSourceType.equals(SourceType.s_const)
                && !rightSourceType.equals(SourceType.s_const)
                && (operator.equals(Operator.less)
                || operator.equals(Operator.lessEqual)
                || operator.equals(Operator.greater)
                || operator.equals(Operator.greaterEqual))) {
            Operator newOperator;
            if (operator.equals(Operator.less))
                newOperator = Operator.greater;
            else if (operator.equals(Operator.greater))
                newOperator = Operator.less;
            else if (operator.equals(Operator.lessEqual))
                newOperator = Operator.greaterEqual;
            else
                newOperator = Operator.lessEqual;

            return new Condition(rightSourceType, rightObject, newOperator, leftSourceType, leftObject);
        } else
            return new Condition(leftSourceType, leftObject, operator, rightSourceType, rightObject);
    }

    private Condition getConditionFromJsonLogic(Operator operator, Object firstItem, Object secondItem) {
        switch (operator) {
            case equal:
            case notEqual:
                return new Condition(convertJsonLogicVariable(firstItem), operator, retrieveCurrentUser(secondItem));
            case greater:
            case greaterEqual:
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
