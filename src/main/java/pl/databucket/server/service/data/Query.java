package pl.databucket.server.service.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import pl.databucket.server.dto.CustomColumnDto;
import pl.databucket.server.dto.DataModifyDTO;
import pl.databucket.server.exception.ConditionNotAllowedException;
import pl.databucket.server.exception.UnknownColumnException;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Query {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Set<String> COLUMNS = Set.of(COL.ID, COL.DATA_ID, COL.TAG_ID, COL.RESERVED, COL.RESERVED_BY,
            COL.PROPERTIES, COL.CREATED_BY, COL.CREATED_AT, COL.MODIFIED_BY, COL.MODIFIED_AT);
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

    public Query selectData(List<CustomColumnDto> columns, Map<String, Object> paramMap) throws UnknownColumnException {
        if (columns != null)
            query = "SELECT " + columnsToString(columns, paramMap);
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

    /**
     * Removes and sets single properties instead of replacing all of them. The paths and values come from the
     * client, so they go into the statement only as parameters (added to paramMap), never as SQL text.
     */
    public Query removeAndSetProperties(boolean execute, DataModifyDTO dataModifyDto, Map<String, Object> paramMap) throws JsonProcessingException {
        if (execute) {
            List<String> propertiesToRemove = dataModifyDto.getPropertiesToRemove();
            Map<String, Object> propertiesToUpdate = dataModifyDto.getPropertiesToSet();
            boolean remove = propertiesToRemove != null && !propertiesToRemove.isEmpty();
            boolean update = propertiesToUpdate != null && !propertiesToUpdate.isEmpty();
            if (remove || update) {
                String properties = "properties";
                if (remove) {
                    for (int i = 0; i < propertiesToRemove.size(); i++)
                        properties += " #- " + jsonPathArray(propertiesToRemove.get(i), "propertyToRemove" + i, paramMap);
                }
                if (update) {
                    Map<String, Object> jsonMap = new HashMap<>();
                    for (Map.Entry<String, Object> prop : propertiesToUpdate.entrySet())
                        updateJson(jsonMap, jsonPathNodes(prop.getKey()), prop.getValue());
                    paramMap.put("propertiesToSet", mapper.writer().writeValueAsString(jsonMap));
                    properties = "jsonb_merge(" + properties + ", CAST(:propertiesToSet AS jsonb))";
                }
                query += (query.contains(" SET ") ? ", " : " SET ") + "properties = " + properties;
            }
        }

        return this;
    }

    // "$.address.city" -> ["address", "city"]
    private static String[] jsonPathNodes(String jsonPath) {
        return (jsonPath.startsWith("$.") ? jsonPath.substring(2) : jsonPath).split("\\.");
    }

    // A path used in a condition, a column or a sort, under the first free parameter name
    private static String jsonPathArray(String jsonPath, Map<String, Object> paramMap) {
        int i = 0;
        while (paramMap.containsKey("path" + i + "_0"))
            i++;
        return jsonPathArray(jsonPath, "path" + i, paramMap);
    }

    // A PostgreSQL text[] of the path nodes, each node a parameter: ARRAY[CAST(:name_0 AS text), ...]
    private static String jsonPathArray(String jsonPath, String name, Map<String, Object> paramMap) {
        String[] nodes = jsonPathNodes(jsonPath);
        List<String> elements = new ArrayList<>();
        for (int i = 0; i < nodes.length; i++) {
            paramMap.put(name + "_" + i, nodes[i]);
            elements.add("CAST(:" + name + "_" + i + " AS text)");
        }
        return "ARRAY[" + String.join(", ", elements) + "]";
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

    private String getConditionStringValue(String uniqueName, SourceType sourceType, Object value, Map<String, Object> paramMap) throws UnknownColumnException {
        if (sourceType.equals(SourceType.s_function)) {
            String sValue = (String) value;
            if (sValue.startsWith("$.")) {
                int lastDot = sValue.lastIndexOf('.');
                String fName = sValue.substring(0, lastDot);
                String functionName = sValue.substring(lastDot + 1);
                switch (functionName) {
                    case "length()":
                        return "jsonb_array_length(properties #> " + jsonPathArray(fName, paramMap) + ")";
                    case "isNotNull()":
                        return "(properties #>> " + jsonPathArray(fName, paramMap) + " is not null)";
                    case "isNull()":
                        return "(properties #>> " + jsonPathArray(fName, paramMap) + " is null)";
                    case "notExists()":
                        return "(properties #> " + jsonPathArray(fName, paramMap) + " is null)";
                    case "exists()":
                        return "(properties #> " + jsonPathArray(fName, paramMap) + " is not null)";
                    default:
                        throw new UnknownColumnException(sValue);
                }
            } else
                return sValue; // SQL given with the "#func:" prefix in the rules
        } else if (sourceType.equals(SourceType.s_property)) {
            String sValue = (String) value;
            if (sValue.startsWith("$."))
                return "properties #> " + jsonPathArray(sValue, paramMap);
            else
                throw new UnknownColumnException(sValue);
        } else if (sourceType.equals(SourceType.s_field)) {
            return checkColumn((String) value);
        } else if (sourceType.equals(SourceType.s_const)) {
            if (value == null)
                return "null";

            if (value instanceof String) {
                Timestamp date = isValidDate((String) value);
                if (date != null)
                    paramMap.put(uniqueName, date);
                else
                    paramMap.put(uniqueName, value);
            } else
                paramMap.put(uniqueName, value);

            return ":" + uniqueName;
        }

        return null;
    }

    // JSON array of the value(s) as a jsonb parameter
    private static String jsonbArrayParam(String uniqueName, Object value, Map<String, Object> paramMap) {
        Collection<?> values = value instanceof Collection ? (Collection<?>) value : Collections.singletonList(value);
        paramMap.put(uniqueName, mapper.valueToTree(values).toString());
        return "CAST(:" + uniqueName + " AS jsonb)";
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
                // COALESCE - avoid situation when missing property used in the condition
                v1 = "COALESCE(properties #> " + jsonPathArray(condition.getRightValue().toString(), paramMap) + ", '[]'::jsonb)";
                op = "@>";
                v2 = jsonbArrayParam(uniqueName + "l", condition.getLeftValue(), paramMap);

                // eg.: $.prop in [1, 2, 3]   >>> v1 in 'v2
            } else if (condition.getLeftSource().equals(SourceType.s_property)) {
                v1 = jsonbArrayParam(uniqueName + "r", condition.getRightValue(), paramMap);
                op = "@>";
                v2 = "(properties #> " + jsonPathArray(condition.getLeftValue().toString(), paramMap) + ")";

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

            if (condition.getLeftSource().equals(SourceType.s_property))
                v1 = "properties #>> " + jsonPathArray((String) condition.getLeftValue(), paramMap);
            else
                v1 = "(" + getConditionStringValue(uniqueName + "l", condition.getLeftSource(), condition.getLeftValue(), paramMap) + ")::varchar";

            op = condition.getOperator().toString();

            if (condition.getRightSource().equals(SourceType.s_property))
                v2 = "properties #>> " + jsonPathArray((String) condition.getRightValue(), paramMap);
            else
                v2 = getConditionStringValue(uniqueName + "r", condition.getRightSource(), "" + condition.getRightValue(), paramMap);

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
                String leftValue = (String) condition.getLeftValue();
                // without "$." it is SQL given with the "#func:" prefix in the rules
                v1 = "(" + (leftValue.startsWith("$.") ? getProperty(leftValue, true, paramMap) : leftValue) + ")";
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

    // "$.path" or "$.path.function()"; asText returns the property as text, otherwise as jsonb
    private String getProperty(String fieldName, boolean asText, Map<String, Object> paramMap) throws UnknownColumnException {
        if (fieldName.endsWith("()")) {
            int lastDot = fieldName.lastIndexOf('.');
            String filedJsonPath = fieldName.substring(0, lastDot);
            String functionName = fieldName.substring(lastDot + 1);

            switch (functionName) {
                case "length()":
                    return "coalesce(jsonb_array_length(properties #> " + jsonPathArray(filedJsonPath, paramMap) + "), 0)";

                case "isNotNull()":
                case "exists()":
                case "isNotEmpty()":
                    return "properties #>> " + jsonPathArray(filedJsonPath, paramMap) + " is not null";

                case "isNull()":
                case "notExists()":
                case "isEmpty()":
                    return "properties #>> " + jsonPathArray(filedJsonPath, paramMap) + " is null";

                default:
                    throw new UnknownColumnException(fieldName);
            }
        } else
            return "properties " + (asText ? "#>> " : "#> ") + jsonPathArray(fieldName, paramMap);
    }

    // A property, or a column the client may refer to by its API name (e.g. "tagId") or its column name
    private String getField(String fieldName, boolean asText, Map<String, Object> paramMap) throws UnknownColumnException {
        if (fieldName.startsWith("$."))
            return getProperty(fieldName, asText, paramMap);
        else
            return checkColumn(convertField(fieldName));
    }

    // Column names come from the client too, so only the bucket table columns get into the SQL
    private static String checkColumn(String column) throws UnknownColumnException {
        if (COLUMNS.contains(column))
            return column;
        else
            throw new UnknownColumnException(column);
    }

    private Timestamp isValidDate(String inDate) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormat.setLenient(false);
        try {
            return new Timestamp(dateFormat.parse(inDate.trim()).getTime());
        } catch (ParseException pe) {
            return null;
        }
    }

    public Query orderBy(String sort, Map<String, Object> paramMap) throws UnknownColumnException {
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
        else if (column.startsWith("$.") && !column.endsWith("()"))
            // a property sorted as jsonb: numbers by value (not 9 after 87 as in text), strings as text; a JSON null
            // as SQL NULL, like a missing property
            orderBy("nullif(" + getProperty(column, false, paramMap) + ", 'null'::jsonb)", asc);
        else
            orderBy(getField(column, true, paramMap), asc);

        return this;
    }

    // columnName is SQL, never a value from the client
    public Query orderBy(String columnName, boolean asc) {
        if (asc) {
            query += " ORDER BY " + columnName;
        } else {
            query += " ORDER BY " + columnName + " desc";
        }
        return this;
    }

    private static String convertField(String field) {
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

    private String columnsToString(String[] columns) {
        return String.join(", ", columns);
    }

    private String columnsToString(List<CustomColumnDto> columns, Map<String, Object> paramMap) throws UnknownColumnException {
        String result = "";
        for (CustomColumnDto col : columns)
            if (col.getTitle() != null)
                result += ", " + getField(col.getField(), false, paramMap) + " as \"" + col.getTitle().replace("\"", "\"\"") + "\"";
            else
                result += ", " + getField(col.getField(), false, paramMap);
        return result.substring(2);
    }

    private String columnsToSetString(Map<String, Object> paramMap) {
        String result = "";
        for (Map.Entry<String, Object> col : paramMap.entrySet())
            result += ", " + col.getKey() + " = :" + col.getKey();
        return result.substring(2);
    }
}
