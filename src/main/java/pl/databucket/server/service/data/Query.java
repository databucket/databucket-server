package pl.databucket.server.service.data;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import pl.databucket.server.dto.CustomColumnDto;
import pl.databucket.server.dto.DataModifyDTO;
import pl.databucket.server.exception.ConditionNotAllowedException;
import pl.databucket.server.exception.UnknownColumnException;

public class Query {

    private static final ObjectMapper mapper = new ObjectMapper();
    String query;
    String table;

    public Query(String table) {
        this.table = table;
    }

    public Query select(String column) throws UnknownColumnException {
        query = "SELECT " + column;
        return this;
    }

    public Query select(String[] columns) throws UnknownColumnException {
        query = "SELECT " + columnsToString(columns);
        return this;
    }

    public Query selectData(List<CustomColumnDto> columns) throws UnknownColumnException {
        if (columns != null)
            query = "SELECT " + columnsToString(columns);
        else
            query = "SELECT *";

        return this;
    }

    public Query delete() {
        query = "DELETE";
        return this;
    }

    public Query update() {
        query = "UPDATE \"" + table + "\"";
        return this;
    }

    public Query set(Map<String, Object> paramMap) {
        if (!paramMap.isEmpty())
            query += " SET " + columnsToSetString(paramMap);
        return this;
    }

    public Query removeAndSetProperties(boolean execute, DataModifyDTO dataModifyDto) throws JsonProcessingException {
        if (execute) {
            List<String> propertiesToRemove = dataModifyDto.getPropertiesToRemove();
            Map<String, Object> propertiesToUpdate = dataModifyDto.getPropertiesToSet();
            if (propertiesToRemove != null && propertiesToRemove.size() > 0 || propertiesToUpdate != null && propertiesToUpdate.size() > 0) {
                if (query.contains("SET"))
                    query += ", properties = ";
                else
                    query += " properties = ";

                if (propertiesToRemove != null && propertiesToRemove.size() > 0 && propertiesToUpdate != null && propertiesToUpdate.size() > 0) {
                    String remove = "properties ";
                    for (String prop : propertiesToRemove)
                        remove += "#- '{" + prop.replace("$.", "").replace(".", ",") + "}' ";

                    Map<String, Object> jsonMap = new HashMap<>();
                    for (Map.Entry<String, Object> prop : propertiesToUpdate.entrySet()) {
                        String[] pathNodes = prop.getKey().replace("$.", "").split("\\.");
                        updateJson(jsonMap, pathNodes, prop.getValue());
                    }
                    query += "jsonb_merge(" + remove + ", '" + mapper.writer().writeValueAsString(jsonMap) + "'::jsonb)";
                } else if (propertiesToRemove != null && propertiesToRemove.size() > 0) {
                    query += "properties ";
                    for (String prop : propertiesToRemove)
                        query += "#- '{" + prop.replace("$.", "").replace(".", ",") + "}' ";
                } else if (propertiesToUpdate != null && propertiesToUpdate.size() > 0) {
                    Map<String, Object> jsonMap = new HashMap<>();
                    for (Map.Entry<String, Object> prop : propertiesToUpdate.entrySet()) {
                        String[] pathNodes = prop.getKey().replace("$.", "").split("\\.");
                        updateJson(jsonMap, pathNodes, prop.getValue());
                    }
                    query += "jsonb_merge(properties, '" + mapper.writer().writeValueAsString(jsonMap) + "'::jsonb)";
                }
            }
        }

        return this;
    }

    public void updateJson(Map<String, Object> map, String[] pathNodes, Object value) {
        String nodeName = pathNodes[0];
        if (pathNodes.length > 1) {
            String[] subPathNodes = Arrays.copyOfRange(pathNodes, 1, pathNodes.length);
            if (map.containsKey(nodeName)) {
                Map<String, Object> subNode = (Map<String, Object>) map.get(nodeName);
                if (subNode != null)
                    updateJson((Map<String, Object>) map.get(nodeName), subPathNodes, value);
                else {
                    subNode = new HashMap<>();
                    map.put(nodeName, subNode);
                    updateJson(subNode, subPathNodes, value);
                }
            } else {
                Map<String, Object> subNode = new HashMap<>();
                map.put(pathNodes[0], subNode);
                updateJson(subNode, subPathNodes, value);
            }
        } else
            map.put(pathNodes[0], value);
    }

    public Query insertIntoValues(MapSqlParameterSource mapParameters) {
        query = "INSERT INTO \"" + table + "\"" + intoValues(mapParameters);
        return this;
    }

    public Query dropTable() {
        query = "DROP TABLE \"" + table + "\"";
        return this;
    }

    public Query from() {
        query += " FROM \"" + table + "\"";
        return this;
    }

    public Query where(QueryRule queryRule, Map<String, Object> paramMap) throws UnknownColumnException, ConditionNotAllowedException {
        String rules = generateQueryRulesString("n", queryRule, paramMap);
        if (rules.length() > 3) // could be () if no rules
            query += " WHERE " + rules;
        return this;
    }

    public Query where(Condition condition, Map<String, Object> paramMap) throws UnknownColumnException, ConditionNotAllowedException {
        if (condition != null) {
            query += " WHERE " + generateConditionString("n", condition, paramMap);
        }
        return this;
    }

    public Query where(List<Condition> conditions, Map<String, Object> paramMap) throws UnknownColumnException, ConditionNotAllowedException {
        if (conditions != null) {
            if (conditions.size() > 1) {
                String condStr = "";
                for (int i = 0; i < conditions.size(); i++) {
                    condStr += generateConditionString("n" + i, conditions.get(i), paramMap) + " and ";
                }
                query += " WHERE " + condStr.substring(0, condStr.length() - " and ".length());
            } else if (conditions.size() > 0)
                where(conditions.get(0), paramMap);
        }
        return this;
    }

    private String getConditionStringValue(String uniqueName, SourceType sourceType, Object value, Map<String, Object> paramMap) {
        if (sourceType.equals(SourceType.s_function)) {
            String sValue = (String) value;
            if (sValue.startsWith("$.") && sValue.endsWith(")")) {
                int lastDot = sValue.lastIndexOf('.');
                String fName = sValue.substring(0, lastDot);
                String functionName = sValue.substring(lastDot + 1);
                switch (functionName) {
                    case "length()":
                        return "jsonb_array_length(properties #> '{" + getPGPropertyArray(fName) + "}')";
                    case "isNotNull()":
                        return "(properties #>> '{" + getPGPropertyArray(fName) + "}' is not null)";
                    case "isNull()":
                        return "(properties #>> '{" + getPGPropertyArray(fName) + "}' is null)";
                    case "notExists()":
                        return "(properties #> '{" + getPGPropertyArray(fName) + "}' is null)";
                    case "exists()":
                        return "(properties #> '{" + getPGPropertyArray(fName) + "}' is not null)";
                    default:
                        return sValue;
                }
            } else
                return sValue;
        } else if (sourceType.equals(SourceType.s_property)) {
            String sValue = (String) value;
            if (sValue.startsWith("$."))
                return "properties #> '{" + getPGPropertyArray(sValue) + "}'";
            else
                return sValue;
        } else if (sourceType.equals(SourceType.s_field)) {
            return (String) value;
        } else if (sourceType.equals(SourceType.s_const)) {
            if (value == null) {
                return "null";
            } else if (paramMap != null) {
                if (value instanceof String) {
                    Timestamp date = isValidDate((String) value);
                    if (date != null)
                        paramMap.put(uniqueName, date);
                    else
                        paramMap.put(uniqueName, value);
                } else
                    paramMap.put(uniqueName, value);

                return ":" + uniqueName;
            } else {
                if (value instanceof String) {
                    return "'" + value + "'";
                } else
                    return "" + value;
            }
        }

        return null;
    }

    private String generateQueryRulesString(String uniqueName, QueryRule queryRule, Map<String, Object> paramMap) throws ConditionNotAllowedException, UnknownColumnException {
        List<String> allConditions = new ArrayList<>();

        if (queryRule.getConditions().size() > 0)
            if (queryRule.getConditions().size() > 1)
                for (int i = 0; i < queryRule.getConditions().size(); i++)
                    allConditions.add(generateConditionString(uniqueName + i, queryRule.getConditions().get(i), paramMap));
            else {
                if (queryRule.getOperator().equals(Operator.not))
                    allConditions.add("not(" + generateConditionString(uniqueName + 0, queryRule.getConditions().get(0), paramMap) + ")");
                else
                    allConditions.add(generateConditionString(uniqueName + 0, queryRule.getConditions().get(0), paramMap));
            }

        if (queryRule.getStrRules().size() > 0)
            allConditions.addAll(queryRule.getStrRules());

        if (queryRule.getQueryRules().size() > 0) {
            if (queryRule.getQueryRules().size() > 1) {
                List<String> qrConditions = new ArrayList<>();
                for (int i = 0; i < queryRule.getQueryRules().size(); i++) {
                    QueryRule qr = queryRule.getQueryRules().get(i);
                    qrConditions.add(generateQueryRulesString(uniqueName + i, qr, paramMap));
                }
                allConditions.add("(" + String.join(" " + queryRule.getOperator().toString() + " ", qrConditions) + ")");
            } else {
                QueryRule qr = queryRule.getQueryRules().get(0);
                if (queryRule.getOperator().equals(Operator.not))
                    allConditions.add("not(" + generateQueryRulesString(uniqueName + 0, qr, paramMap) + ")");
                else
                    allConditions.add(generateQueryRulesString(uniqueName + 0, qr, paramMap));
            }
        }

        return "(" + String.join(" " + queryRule.getOperator().toString() + " ", allConditions) + ")";
    }

    private String generateConditionString(String uniqueName, Condition condition, Map<String, Object> paramMap) throws ConditionNotAllowedException, UnknownColumnException {
        String sFormat = "%s %s %s";
        String v1;
        String op;
        String v2;

        if (condition.getOperator().equals(Operator.in) || condition.getOperator().equals(Operator.notIn)) {

            if (condition.getOperator().equals(Operator.notIn))
                sFormat = "NOT(%s %s (%s))";
            else
                sFormat = "%s %s (%s)";

            // eg.: $.textValue in $.textArray
            if (condition.getLeftSource().equals(SourceType.s_property) && condition.getRightSource().equals(SourceType.s_property)) {
                throw new ConditionNotAllowedException(condition);

                // eg.: 5 in $.jsonArray   >>> v1 @> '[v2]'
            } else if (condition.getRightSource().equals(SourceType.s_property)) {
                v1 = "properties #> '{" + getPGPropertyArray(condition.getRightValue().toString()) + "}'";
                op = "@>";
                if (condition.getLeftValue() instanceof String) {
                    v2 = "'[\"" + condition.getLeftValue() + "\"]'";
                } else if (condition.getLeftValue() instanceof ArrayList<?>) {
                    v2 = "'[" + getPGStringArray((ArrayList<?>) condition.getLeftValue()) + "]'";
                } else
                    v2 = "'[" + condition.getLeftValue() + "]'";

                // eg.: $.prop in [1, 2, 3]   >>> v1 in 'v2
            } else if (condition.getLeftSource().equals(SourceType.s_property)) {
                if (condition.getRightValue() instanceof ArrayList)
                    v1 = "'[" + getPGStringArray((ArrayList<?>) condition.getRightValue()) + "]'";
                else {
                    if (condition.getRightValue() instanceof String) {
                        v1 = "'[\"" + condition.getRightValue() + "\"]'";
                    } else {
                        v1 = "'[" + condition.getRightValue() + "]'";
                    }
                }
                op = "@>";
                v2 = "(properties #> '{" + getPGPropertyArray(condition.getLeftValue().toString()) + "}')";

                // property not used in this condition
            } else {
                v1 = getConditionStringValue(uniqueName + "1", condition.getLeftSource(), condition.getLeftValue(), paramMap);
                op = Operator.in.toString();
                v2 = getConditionStringValue(uniqueName + "2", condition.getRightSource(), condition.getRightValue(), paramMap);
            }
        }

        // LIKE, NOT LIKE, SIMILAR TO, NOT SIMILAR TO, MATCHES, NOT MATCH operator
        else if (condition.getOperator().equals(Operator.like)
                || condition.getOperator().equals(Operator.notLike)
                || condition.getOperator().equals(Operator.similarTo)
                || condition.getOperator().equals(Operator.notSimilarTo)
                || condition.getOperator().equals(Operator.matchesCI)
                || condition.getOperator().equals(Operator.notMatchCI)
                || condition.getOperator().equals(Operator.matchesCS)
                || condition.getOperator().equals(Operator.notMatchCS)) {

            String leftValue, rightValue;

            if (condition.getLeftSource().equals(SourceType.s_property))
                leftValue = "properties #>> '{" + getPGPropertyArray((String) condition.getLeftValue()) + "}'";
            else
                leftValue = "(" + condition.getLeftValue() + ")::varchar";

            if (condition.getRightSource().equals(SourceType.s_property))
                rightValue = "properties #>> '{" + getPGPropertyArray((String) condition.getRightValue()) + "}'";
            else
                rightValue = "" + condition.getRightValue();


            v1 = getConditionStringValue(uniqueName + "l", condition.getLeftSource(), leftValue, paramMap);
            op = condition.getOperator().toString();
            v2 = getConditionStringValue(uniqueName + "r", condition.getRightSource(), rightValue, paramMap);

        } else {
            boolean convertToDateTime = false;
            if (condition.getLeftSource().equals(SourceType.s_property)) {
                if (condition.getRightValue() == null)
                    v1 = getConditionStringValue(uniqueName + "l", condition.getLeftSource(), condition.getLeftValue(), paramMap);
                else if (condition.getRightValue() instanceof Integer)
                    v1 = "(" + getConditionStringValue(uniqueName + "l", condition.getLeftSource(), condition.getLeftValue(), paramMap) + ")::int";
                else if (condition.getRightValue() instanceof Float)
                    v1 = "(" + getConditionStringValue(uniqueName + "l", condition.getLeftSource(), condition.getLeftValue(), paramMap) + ")::float";
                else if (condition.getRightValue() instanceof Boolean)
                    v1 = "(" + getConditionStringValue(uniqueName + "l", condition.getLeftSource(), condition.getLeftValue(), paramMap) + ")::bool";
                else if (isValidDate((String) condition.getRightValue()) != null) {
                    convertToDateTime = true;
                    String leftValue = ((String) condition.getLeftValue()).replace("::timestamp", ""); // in case using optional cast
                    v1 = "(" + getConditionStringValue(uniqueName + "l", condition.getLeftSource(), leftValue, paramMap) + ")::text::timestamp";
                } else {
                    String leftValue = (String) condition.getLeftValue();
                    if (leftValue.endsWith("::timestamp")) {
                        leftValue = leftValue.substring(0, leftValue.indexOf("::timestamp"));
                        v1 = "(" + getConditionStringValue(uniqueName + "l", condition.getLeftSource(), leftValue, paramMap) + ")::text::timestamp";
                    } else
                        v1 = "(" + getConditionStringValue(uniqueName + "l", condition.getLeftSource(), leftValue, paramMap) + ")::text";
                }
            } else if (condition.getLeftSource().equals(SourceType.s_function)) {
                v1 = "(" + getField((String) condition.getLeftValue()) + ")";
            } else
                v1 = getConditionStringValue(uniqueName + "l", condition.getLeftSource(), condition.getLeftValue(), paramMap);

            op = condition.getOperator().toString();

            if (condition.getLeftSource().equals(SourceType.s_property)) {
                if (condition.getRightValue() == null)
                    v2 = null;
                else if (convertToDateTime) {
                    Timestamp timestamp = isValidDate((String) condition.getRightValue());
                    v2 = getConditionStringValue(uniqueName + "r", condition.getRightSource(), timestamp, paramMap);
                } else if (condition.getRightValue() instanceof String && condition.getRightSource() != SourceType.s_function)
                    v2 = getConditionStringValue(uniqueName + "r", condition.getRightSource(), "\"" + condition.getRightValue() + "\"", paramMap);
                else
                    v2 = getConditionStringValue(uniqueName + "r", condition.getRightSource(), condition.getRightValue(), paramMap);
            } else
                v2 = getConditionStringValue(uniqueName + "r", condition.getRightSource(), condition.getRightValue(), paramMap);
        }

        return String.format(sFormat, v1, op, v2);
    }

    // returns PGObjects instead of strings
    private String getField4Select(String fieldName) throws UnknownColumnException {
        if (fieldName.startsWith("$.")) {
            if (fieldName.endsWith("()")) {
                int lastDot = fieldName.lastIndexOf('.');
                String filedJsonPath = fieldName.substring(0, lastDot);
                String functionName = fieldName.substring(lastDot + 1);
                String resultFiled;

                switch (functionName) {
                    case "length()":
                        resultFiled = "coalesce(jsonb_array_length(properties #> '{" + getPGPropertyArray(filedJsonPath) + "}'), 0)";
                        break;

                    case "isNotNull()":
                    case "exists()":
                    case "isNotEmpty()":
                        resultFiled = "properties #>> '{" + getPGPropertyArray(filedJsonPath) + "}' is not null";
                        break;

                    case "isNull()":
                    case "notExists()":
                    case "isEmpty()":
                        resultFiled = "properties #>> '{" + getPGPropertyArray(filedJsonPath) + "}' is null";
                        break;

                    default:
                        throw new UnknownColumnException(fieldName);
                }

                return resultFiled;

            } else
                return "properties #> '{" + getPGPropertyArray(fieldName) + "}'";

        } else
            return fieldName;
    }

    private String getField(String fieldName) throws UnknownColumnException {
        if (fieldName.startsWith("$.")) {
            if (fieldName.endsWith("()")) {
                int lastDot = fieldName.lastIndexOf('.');
                String filedJsonPath = fieldName.substring(0, lastDot);
                String functionName = fieldName.substring(lastDot + 1);
                String resultFiled;

                switch (functionName) {
                    case "length()":
                        resultFiled = "coalesce(jsonb_array_length(properties #> '{" + getPGPropertyArray(filedJsonPath) + "}'), 0)";
                        break;

                    case "isNotNull()":
                    case "exists()":
                    case "isNotEmpty()":
                        resultFiled = "properties #>> '{" + getPGPropertyArray(filedJsonPath) + "}' is not null";
                        break;

                    case "isNull()":
                    case "notExists()":
                    case "isEmpty()":
                        resultFiled = "properties #>> '{" + getPGPropertyArray(filedJsonPath) + "}' is null";
                        break;

                    default:
                        throw new UnknownColumnException(fieldName);
                }

                return resultFiled;

            } else
                return "properties #>> '{" + getPGPropertyArray(fieldName) + "}'";

        } else
            return fieldName;
    }

    private String getPGPropertyArray(String jsonPath) {
        // $.group.subgroup.item 	>>>>  group,subgroup,item
        String result = jsonPath.substring(2);
        result = result.replace(".", ",");
        return result;
    }

    // returns eg.: "text1","text2","text3"
    private String getPGStringArray(ArrayList<?> list) {
        StringBuilder result = new StringBuilder();
        for (Object item : list)
            if (item instanceof String)
                result.append(",\"").append(item).append("\"");
            else
                result.append(",").append(item);
        return result.substring(1); // cut first comma character
    }

    private java.sql.Timestamp isValidDate(String inDate) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setLenient(false);
        try {
            return new java.sql.Timestamp(dateFormat.parse(inDate.trim()).getTime());
        } catch (ParseException pe) {
            return null;
        }
    }

    public Query orderBy(Optional<String> sort) throws UnknownColumnException {
        if (sort.isPresent())
            return orderBy(sort.get());
        else
            return this;
    }

    public Query orderBy(String sort) throws UnknownColumnException {
        boolean asc = true;
        String column = sort;

        if (sort.startsWith("desc(")) {
            column = column.replace("desc(", "");
            column = column.substring(0, column.length() - 1);
            asc = false;
        } else if (sort.startsWith("asc(")) {
            column = column.replace("asc(", "");
            column = column.substring(0, column.length() - 1);
        }

        if (column.toUpperCase().startsWith("RANDOM"))
            orderBy("RANDOM()", true);
        else
            orderBy(getField(column), asc);

        return this;
    }

    public Query orderBy(String columnName, boolean asc) {
        if (asc) {
            query += " ORDER BY " + convertField(columnName);
        } else {
            query += " ORDER BY " + convertField(columnName) + " desc";
        }
        return this;
    }

    private String convertField(String field) {
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

    public Query limit(Map<String, Object> paramMap, int limit) {
        query += " LIMIT :limit";
        paramMap.put("limit", limit);
        return this;
    }

    public Query limitPage(Map<String, Object> paramMap, Integer limit, Integer page) {
        limit(paramMap, limit);
        offset(paramMap, page * limit - limit);
        return this;
    }

    public Query forUpdateSkipLocked() {
        query += " for update skip locked";
        return this;
    }

    public void offset(Map<String, Object> paramMap, int offset) {
        query += " OFFSET :offset";
        paramMap.put("offset", offset);
    }

    public String toString(Logger logger, Map<String, Object> paramMap) {
        if (paramMap != null) {
            logger.debug("paramMap: " + convertWithStream(paramMap));
        }
        logger.debug(query);
        return query;
    }

    public String convertWithStream(Map<String, Object> map) {
        return map.keySet().stream()
                .map(key -> key + (map.get(key) != null ? "(" + map.get(key).getClass().getSimpleName() + ")=" : "=") + map.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private String intoValues(MapSqlParameterSource mapParameters) {
        String columns = "";
        String values = "";
        for (Map.Entry<String, Object> prop : mapParameters.getValues().entrySet()) {
            columns += ", \"" + prop.getKey() + "\"";
            values += ", :" + prop.getKey();
        }
        return " (" + columns.substring(2) + ") VALUES (" + values.substring(2) + ")";
    }

    private String columnsToString(String[] columns) throws UnknownColumnException {
        String result = "";
        for (String col : columns)
            result += ", " + getField4Select(col);
        return result.substring(2);
    }

    private String columnsToString(List<CustomColumnDto> columns) throws UnknownColumnException {
        String result = "";
        for (CustomColumnDto col : columns)
            if (col.getTitle() != null)
                result += ", " + getField4Select(col.getField()) + " as \"" + col.getTitle() + "\"";
            else
                result += ", " + getField4Select(col.getField());
        return result.substring(2);
    }

    private String columnsToSetString(Map<String, Object> paramMap) {
        String result = "";
        for (Map.Entry<String, Object> col : paramMap.entrySet())
            result += ", " + col.getKey() + " = :" + col.getKey();
        return result.substring(2);
    }
}
