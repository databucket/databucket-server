package pl.databucket.server.service.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import pl.databucket.server.dto.CustomColumnDto;
import pl.databucket.server.dto.DataModifyDTO;
import pl.databucket.server.exception.UnknownColumnException;

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

    /** A WHERE condition built by {@link pl.databucket.server.service.data.filter.LogicSql}; null for none. */
    public Query whereSql(String condition) {
        if (condition != null && !condition.isBlank())
            query += " WHERE " + condition;
        return this;
    }

    /**
     * A WHERE condition of the server's own: columns of the code (never names from the client) equal to the values,
     * or one of them for a collection, combined with AND. The values are parameters (w0, w1, ...).
     */
    public Query where(Map<String, Object> columnValues, Map<String, Object> paramMap) throws UnknownColumnException {
        List<String> parts = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Object> entry : columnValues.entrySet()) {
            String column = checkColumn(entry.getKey());
            Object value = entry.getValue();
            String name = "w" + i++;
            if (value == null)
                parts.add(column + " IS NULL");
            else if (value instanceof Collection<?> values && values.isEmpty())
                parts.add("FALSE");
            else {
                paramMap.put(name, value);
                parts.add(column + (value instanceof Collection ? " IN (:" + name + ")" : " = :" + name));
            }
        }
        return whereSql(String.join(" AND ", parts));
    }

    /** As {@link #where(Map, Map)} for one column. */
    public Query where(String column, Object value, Map<String, Object> paramMap) throws UnknownColumnException {
        return where(Collections.singletonMap(column, value), paramMap);
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
