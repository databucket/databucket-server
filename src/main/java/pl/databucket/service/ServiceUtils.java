package pl.databucket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import pl.databucket.database.*;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.Tag;
import pl.databucket.exception.ConditionNotAllowedException;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.UnexpectedException;
import pl.databucket.exception.UnknownColumnException;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServiceUtils {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    Logger logger;

    public ServiceUtils(NamedParameterJdbcTemplate jdbcTemplate, Logger logger) {
        this.jdbcTemplate = jdbcTemplate;
        this.logger = logger;
    }

    public List<Map<String, Object>> getBuckets(Integer bucketId, Integer classId) throws UnknownColumnException, ConditionNotAllowedException {
        Map<String, Object> paramMap = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (bucketId != null && bucketId != -1)
            conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));

        if (classId != null && classId != -1)
            conditions.add(new Condition(COL.CLASS_ID, Operator.equal, classId));

        Query queryData = new Query(TAB.BUCKET, false)
                .select(getBucketColumns())
                .from()
                .where(conditions, paramMap);

        return jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap);
    }

    public int getBucketId(String bucketName) throws ItemNotFoundException, UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.BUCKET_NAME, Operator.equal, bucketName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.BUCKET, true)
                .select(COL.BUCKET_ID)
                .from()
                .where(conditions, paramMap);

        try {
            return jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            throw new ItemNotFoundException(Bucket.class, bucketName);
        }
    }

    public Map<String, Object> getView(int viewId) throws UnexpectedException, UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.VIEW_ID, Operator.equal, viewId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.VIEW, true)
                .select(getViewColumns())
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> views = this.jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
        if (views != null && views.size() > 0) {
            try {
                convertStringToList(views, COL.CONDITIONS);
                convertStringToList(views, COL.COLUMNS);
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
            return views.get(0);
        } else
            return null;
    }


    public List<Condition> getFilterConditions(int filterId) throws UnexpectedException, UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        Map<String, Object> filter = getFilter(filterId);
        List<Map<String, Object>> filterConditions = (List<Map<String, Object>>) filter.get(COL.CONDITIONS);
        for (Map<String, Object> filterCond : filterConditions)
            conditions.add(new Condition(filterCond));
        return conditions;
    }

    public Map<String, Object> getFilter(int filterId) throws UnexpectedException, UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.FILTER_ID, Operator.equal, filterId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.FILTER, true)
                .select(getFilterColumns())
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> filters = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);

        if (filters != null && filters.size() > 0) {
            try {
                convertStringToList(filters, COL.CONDITIONS);
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
            return filters.get(0);
        } else
            return null;
    }

    public int getTagId(String tagName) throws ItemNotFoundException, UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.TAG_NAME, Operator.equal, tagName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.TAG, true)
                .select(COL.TAG_ID)
                .from()
                .where(conditions, paramMap);

        try {
            return jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            throw new ItemNotFoundException(Tag.class, tagName);
        }
    }


    // when properties items are using in selected columns (e.g. select properties->'$.item' from bucket) then MySQL keep all data as Strings. This method converts items into proper types.
    public void convertPropertiesColumns(List<Map<String, Object>> source) {
        for (Map<String, Object> map : source) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() != null) {
                    String value = "" + entry.getValue();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        entry.setValue(value.substring(1, value.length() - 1));
                    } else if (value.equals("true")) {
                        entry.setValue(true);
                    } else if (value.equals("false")) {
                        entry.setValue(false);
                    } else if (value.equals("null")) {
                        entry.setValue(null);
                    } else {
                        try {
                            Integer integerValue = Integer.parseInt(value);
                            entry.setValue(integerValue);
                        } catch (NumberFormatException e1) {
                            try {
                                Double doubleValue = Double.parseDouble(value);
                                entry.setValue(doubleValue);
                            } catch (NumberFormatException e2) {
                                // do nothing
                            }
                        }
                    }
                }
            }
        }
    }

    public void convertStringToMap(List<Map<String, Object>> source, String targetItemName) throws UnexpectedException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (Map<String, Object> itemMap : source) {
                String targetItemValueStr = (String) itemMap.get(targetItemName);
                if (targetItemValueStr != null) {
                    Map<String, Object> targetItemMap = mapper.readValue(targetItemValueStr, new TypeReference<Map<String, Object>>() {
                    });
                    itemMap.put(targetItemName, targetItemMap);
                }
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public void convertStringToList(List<Map<String, Object>> source, String targetItemName) throws UnexpectedException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (Map<String, Object> itemMap : source) {
                String targetItemValueStr = (String) itemMap.get(targetItemName);
                if (targetItemValueStr != null) {
                    List<Map<String, Object>> targetItemList = mapper.readValue(targetItemValueStr, new TypeReference<List<Map<String, Object>>>() {
                    });
                    itemMap.put(targetItemName, targetItemList);
                }
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getTaskModifyQuery(String eventName, String bucketName, List<Condition> conditions, Map<String, Object> actions) throws UnknownColumnException, ConditionNotAllowedException {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(COL.UPDATED_BY, eventName);

        boolean setTag = (boolean) actions.get(C.SET_TAG);
        if (setTag) {
            int tagId = (int) actions.get(C.TAG_ID);
            paramMap.put(COL.TAG_ID, tagId);
        }

        boolean setLock = (boolean) actions.get(C.SET_LOCK);
        if (setLock) {
            boolean lock = (boolean) actions.get(C.LOCK);
            paramMap.put(COL.LOCKED, lock);

            if (!lock)
                paramMap.put(COL.LOCKED_BY, null);
        }

        boolean joinTags = usedTagNameColumn(conditions);
        Map<String, Object> propertiesMap = getTaskActionsProperties(actions);

        Query query = new Query(bucketName, true)
                .update()
                .joinTags(joinTags)
                .setWithValues(paramMap)
                .removeAndSetProperties(true, propertiesMap)
                .where(conditions);

        return query.toString(logger, paramMap);
    }

    public Map<String, Object> getTaskActionsProperties(Map<String, Object> actions) {
        Map<String, Object> resultMap = new HashMap<>();

        List<Map<String, Object>> actionProperties = (List<Map<String, Object>>) actions.get(C.PROPERTIES);
        if (actionProperties != null && actionProperties.size() > 0) {
            List<String> propertiesToRemoveArray = new ArrayList<>();
            Map<String, Object> propertiesToModifyMap = new HashMap<>();

            for (Map<String, Object> item : actionProperties) {
                String action = (String) item.get(C.ACTION);
                String path = (String) item.get(C.PATH);
                if (action.equals(C.REMOVE))
                    propertiesToRemoveArray.add(path);
                else if (action.equals(C.SET)) {
                    String type = (String) item.get(C.TYPE);
                    String value = (String) item.get(C.VALUE);
                    switch (type) {
                        case "numeric":
                            propertiesToModifyMap.put(path, Float.parseFloat(value));
                            break;
                        case "boolean":
                            propertiesToModifyMap.put(path, value.equalsIgnoreCase("TRUE"));
                            break;
                        case "null":
                            propertiesToModifyMap.put(path, null);
                            break;
                        default:
                            propertiesToModifyMap.put(path, value);
                            break;
                    }
                }
            }

            if (propertiesToModifyMap.size() > 0)
                resultMap.put("update_properties", propertiesToModifyMap);

            if (propertiesToRemoveArray.size() > 0)
                resultMap.put("remove_properties", propertiesToRemoveArray);
        }

        return resultMap;
    }

    public boolean isTaskExist(Integer taskId) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.TASK_ID, Operator.equal, taskId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.TASK, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);

        return count > 0;
    }

    public boolean isFilterExist(Integer filterId) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.FILTER_ID, Operator.equal, filterId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.FILTER, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);

        return count > 0;
    }

    public boolean isFilterExist(String filterName) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.FILTER_NAME, Operator.equal, filterName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.FILTER, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);

        return count > 0;
    }

    public Map<String, Object> getTask(int taskId) throws UnknownColumnException, UnexpectedException, ConditionNotAllowedException {
        Map<String, Object> paramMap = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
        conditions.add(new Condition(COL.TASK_ID, Operator.equal, taskId));

        Query queryData = new Query(TAB.TASK, true)
                .select(getTaskColumns())
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> taskList = jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap);
        convertStringToMap(taskList, COL.CONFIGURATION);
        return taskList.get(0);
    }

    public String getTaskRemoveQuery(String bucketName, List<Condition> conditions) throws UnknownColumnException, ConditionNotAllowedException {
        boolean joinTags = usedTagNameColumn(conditions);

        Query query = new Query(bucketName, true)
                .delete()
                .from()
                .joinTags(joinTags)
                .where(conditions);

        return query.toString(logger, null);
    }

    public boolean isDatabaseEventExist(String eventName) {
        String query = "SHOW EVENTS WHERE name = '" + eventName + "'";

        logger.debug(query);
        List<Map<String, Object>> result = jdbcTemplate.getJdbcTemplate().queryForList(query);

        return result.size() > 0;
    }

    public List<Map<String, Object>> getDatabaseEvents() {
//        String query = "SHOW EVENTS";
//
//        logger.debug(query);
//
//        return jdbcTemplate.getJdbcTemplate().queryForList(query);
        return new ArrayList<>();
    }

    public List<String> getDatabaseEventsNames() {
        List<Map<String, Object>> events = getDatabaseEvents();
        List<String> names = new ArrayList<>();
        for (Map<String, Object> event : events)
            names.add((String) event.get("name"));

        return names;
    }

    public boolean isItemExist(Integer id) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.EVENT_ID, Operator.equal, id));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.EVENT, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);

        return count > 0;
    }

    public boolean isColumnsExist(Integer columnsId) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.COLUMNS_ID, Operator.equal, columnsId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.COLUMNS, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);

        return count > 0;
    }

    public boolean isColumnsExist(String columnsName) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.COLUMNS_NAME, Operator.equal, columnsName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.COLUMNS, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);

        return count > 0;
    }

    public boolean isClassExist(Integer classId, String className) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.CLASS_NAME, Operator.equal, className));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (classId != null)
            conditions.add(new Condition(COL.CLASS_ID, Operator.notEqual, classId));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.CLASS, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);

        return count > 0;
    }

    public boolean isClassExist(Integer classId) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.CLASS_ID, Operator.equal, classId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.CLASS, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);

        return count > 0;
    }

    public boolean isClassExist(String className) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.CLASS_NAME, Operator.equal, className));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.CLASS, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);

        return count > 0;
    }

    public boolean isEventExist(Integer eventId, String eventName) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.EVENT_NAME, Operator.equal, eventName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (eventId != null)
            conditions.add(new Condition(COL.EVENT_ID, Operator.notEqual, eventId));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.EVENT, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);

        return count > 0;
    }

    public boolean putNotEmpty(MapSqlParameterSource map, String name, Object value) throws JsonProcessingException, SQLException {
        if (value != null) {
            if (value instanceof LinkedHashMap || value instanceof ArrayList)
                map.addValue(name, javaObjectToPGObject(value));
            else
                map.addValue(name, value);
            return true;
        }
        return false;
    }

    public boolean putNotEmpty(Map<String, Object> source, Map<String, Object> paramMap, String key) throws JsonProcessingException, SQLException {
        if (source.containsKey(key)) {
            Object value = source.get(key);
            if (value instanceof LinkedHashMap || value instanceof ArrayList)
                paramMap.put(key, javaObjectToPGObject(value));
            else
                paramMap.put(key, value);
            return true;
        }
        return false;
    }

    public void putNotEmpty(Map<String, Object> paramMap, String name, Object value) throws JsonProcessingException, SQLException {
        if (value != null) {
            if (value instanceof Map || value instanceof List)
                paramMap.put(name, javaObjectToPGObject(value));
            else
                paramMap.put(name, value);
        }
    }

    // -1 means null
    public void putNotEmptyNullableInteger(Map<String, Object> paramMap, String name, Object value) {
        if (value != null) {
            Integer vInt = (Integer) value;
            if (vInt != -1)
                paramMap.put(name, vInt);
            else
                paramMap.put(name, null);
        }
    }

    public boolean usedTagNameColumn(List<Condition> urlConditions) {
        if (urlConditions != null)
            for (Condition condition : urlConditions)
                if (condition.getLeftValue().equals(COL.TAG_NAME))
                    return true;
        return false;
    }

    public String getColumnName(String message) {
        Pattern pattern = Pattern.compile("'b\\.(.+?)'");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        } else
            return null;
    }

    public PGobject javaObjectToPGObject(Object object) throws JsonProcessingException, SQLException {
        String jsonObjectAsStr = new ObjectMapper().writeValueAsString(object);
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        pgObject.setValue(jsonObjectAsStr);

        return pgObject;
    }

    public String getFormattedDate(String dateInString) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        LocalDateTime dt = LocalDateTime.parse(dateInString, fmt);
        Instant iDate = dt.toInstant(ZoneOffset.UTC);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.systemDefault());
        return dateTimeFormatter.format(iDate);
    }

    public String getUnit(int key) {
        switch (key) {
            case 1:
                return "MINUTE";
            case 2:
                return "HOUR";
            case 3:
                return "DAY";
            case 4:
                return "WEEK";
            case 5:
                return "MONTH";
            default:
                return null;
        }
    }

    public String[] getBucketColumns() {
        return new String[]{COL.BUCKET_ID, COL.BUCKET_NAME, COL.CLASS_ID, COL.INDEX, COL.DESCRIPTION, COL.ICON_NAME, COL.HISTORY, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
    }

    public String[] getViewColumns() {
        return new String[]{COL.VIEW_ID, COL.VIEW_NAME, COL.DESCRIPTION, COL.CLASS_ID, COL.BUCKET_ID, COL.FILTER_ID, COL.COLUMNS_ID, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
    }

    public String[] getColumnsColumns() {
        return new String[]{COL.COLUMNS_ID, COL.COLUMNS_NAME, COL.CLASS_ID, COL.BUCKET_ID, COL.COLUMNS + "::varchar", COL.DESCRIPTION, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
    }

    public String[] getClassColumns() {
        return new String[]{COL.CLASS_ID, COL.CLASS_NAME, COL.DESCRIPTION, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
    }

    public String[] getEventsColumns() {
        return new String[]{COL.EVENT_ID, COL.EVENT_NAME, COL.CLASS_ID, COL.BUCKET_ID, COL.DESCRIPTION, COL.SCHEDULE + "::varchar", COL.TASKS + "::varchar", COL.ACTIVE, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
    }

    public String[] getEventsLogColumns() {
        return new String[]{COL.EVENT_LOG_ID, COL.EVENT_ID, COL.TASK_ID, COL.BUCKET_ID, COL.AFFECTED, COL.CREATED_AT};
    }

    public String[] getTaskColumns() {
        return new String[]{COL.TASK_ID, COL.TASK_NAME, COL.CLASS_ID, COL.BUCKET_ID, COL.DESCRIPTION, COL.CONFIGURATION + "::varchar", COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
    }

    public String[] getFilterColumns() {
        return new String[]{COL.FILTER_ID, COL.FILTER_NAME, COL.CLASS_ID, COL.BUCKET_ID, COL.CONDITIONS + "::varchar", COL.DESCRIPTION, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
    }

    public String[] getGroupColumns() {
        return new String[]{COL.GROUP_ID, COL.GROUP_NAME, COL.DESCRIPTION, COL.BUCKETS + "::varchar", COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
    }

    public String[] getTagColumns() {
        return new String[]{COL.TAG_ID, COL.TAG_NAME, COL.CLASS_ID, COL.BUCKET_ID, COL.DESCRIPTION, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
    }

}
