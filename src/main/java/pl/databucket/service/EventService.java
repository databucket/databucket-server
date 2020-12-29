package pl.databucket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import pl.databucket.old_service.ServiceUtils;
import pl.databucket.database.*;
import pl.databucket.exception.*;

import java.sql.SQLException;
import java.util.*;


@Service
public class EventService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ServiceUtils serviceUtils;
    Logger logger = LoggerFactory.getLogger(EventService.class);

    public EventService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.serviceUtils = new ServiceUtils(jdbcTemplate, logger);
    }

    public int createEvent(String eventName, Integer bucketId, Integer classId, String userName, String description, Map<String, Object> schedule, List<Map<String, Object>> tasks, Boolean active) throws JsonProcessingException, DataAccessException, UnknownColumnException, ItemAlreadyExistsException, UnexpectedException, EmptyInputValueException, SQLException, ConditionNotAllowedException {

        // check an event exist in the _event table
        if (serviceUtils.isEventExist(null, eventName))
            throw new ItemAlreadyExistsException("Event", eventName);

        // check an event exist in the database events
        if (serviceUtils.isDatabaseEventExist(eventName))
            throw new ItemAlreadyExistsException("Event " + eventName + " already exist in the database!");

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue(COL.EVENT_NAME, eventName);
        namedParameters.addValue(COL.CREATED_BY, userName);

        namedParameters.addValue(COL.SCHEDULE, serviceUtils.javaObjectToPGObject(schedule));
        namedParameters.addValue(COL.TASKS, serviceUtils.javaObjectToPGObject(tasks));
        serviceUtils.putNotEmpty(namedParameters, COL.DESCRIPTION, description);
        serviceUtils.putNotEmpty(namedParameters, COL.ACTIVE, active);
        serviceUtils.putNotEmpty(namedParameters, COL.CLASS_ID, classId);
        serviceUtils.putNotEmpty(namedParameters, COL.BUCKET_ID, bucketId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(TAB.EVENT, true).insertIntoValues(namedParameters);
        jdbcTemplate.update(query.toString(logger, null), namedParameters, keyHolder, new String[]{COL.EVENT_ID});

        int eventId = keyHolder.getKey().intValue();

        if (active != null && active) {
            List<Map<String, Object>> buckets = serviceUtils.getBuckets(bucketId, classId);
            Map<String, Object> event = getEvent(eventId);
            String eName = (String) event.get(COL.EVENT_NAME);
            createDatabaseEvent(eventId, eName, buckets, schedule, tasks);
        }

        return eventId;
    }

    private Map<String, Object> getEvent(int eventId) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.EVENT_ID, Operator.equal, eventId));
        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.EVENT, true)
                .select(serviceUtils.getEventsColumns())
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> eventList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
        if (eventList.size() > 0)
            return eventList.get(0);
        else
            return null;
    }

    public Map<ResultField, Object> getEvents(Optional<String> bucketName, Optional<Integer> eventId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException, UnexpectedException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        Map<String, Object> paramMap = new HashMap<>();

        if (eventId.isPresent()) {
            conditions.add(new Condition(COL.EVENT_ID, Operator.equal, eventId.get()));
        } else if (bucketName.isPresent()) {
            conditions.add(new Condition(COL.BUCKET_NAME, Operator.equal, bucketName.get()));
        }

        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (urlConditions != null)
            conditions.addAll(urlConditions);

        Query queryCount = new Query(TAB.EVENT, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.EVENT, true)
                .select(serviceUtils.getEventsColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<ResultField, Object> result = new HashMap<>();
        result.put(ResultField.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger, paramMap), paramMap, Long.TYPE));
        List<Map<String, Object>> eventList = jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap);

        // Verify active events. All active events should have its representation in database events.
        List<String> databaseEvents = serviceUtils.getDatabaseEventsNames();
        List<Integer> removedEventsIds = new ArrayList<>();

        for (Map<String, Object> eventItem : eventList) {
            if ((boolean) eventItem.get(COL.ACTIVE)) {
                String eventName = (String) eventItem.get(COL.EVENT_NAME);
                if (!databaseEvents.contains(eventName))
                    removedEventsIds.add((Integer) eventItem.get(COL.EVENT_ID));
            }
        }

        if (removedEventsIds.size() > 0) {
            deactivateEvents(removedEventsIds);
            eventList = jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap);
        }

        serviceUtils.convertStringToList(eventList, COL.TASKS);
        serviceUtils.convertStringToMap(eventList, COL.SCHEDULE);
        result.put(ResultField.DATA, eventList);

        return result;
    }

    public void modifyEvent(String userName, Integer eventId, String eventName, Integer bucketId, Integer classId, String description, Map<String, Object> schedule, List<Map<String, Object>> tasks, Boolean active) throws UnexpectedException, UnknownColumnException, ItemDoNotExistsException, ItemAlreadyExistsException, EmptyInputValueException, ConditionNotAllowedException {
        Map<String, Object> event = getEvent(eventId);
        if (event != null) {

            // check an event exist in the _event table
            if (eventName != null && serviceUtils.isEventExist(eventId, eventName))
                throw new ItemAlreadyExistsException("Event", eventName);

            // check an event exist in the database events
            if (eventName != null && serviceUtils.isDatabaseEventExist(eventName))
                throw new ItemAlreadyExistsException("Event " + eventName + " already exist in the database!");

            List<Map<String, Object>> buckets = null;
            if (active) {
                buckets = serviceUtils.getBuckets(bucketId, classId);
                if (buckets.size() == 0)
                    throw new ItemDoNotExistsException("No bucket found for this configuration!");
            }

            // Remove old event
            String eName = (String) event.get(COL.EVENT_NAME);
            removeDatabaseEvent(eName);

            Condition condition = new Condition(COL.EVENT_ID, Operator.equal, eventId);
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put(COL.UPDATED_BY, userName);
            try {
                serviceUtils.putNotEmptyNullableInteger(paramMap, COL.BUCKET_ID, bucketId);
                serviceUtils.putNotEmptyNullableInteger(paramMap, COL.CLASS_ID, classId);
                serviceUtils.putNotEmpty(paramMap, COL.EVENT_NAME, eventName);
                serviceUtils.putNotEmpty(paramMap, COL.DESCRIPTION, description);
                serviceUtils.putNotEmpty(paramMap, COL.SCHEDULE, schedule);
                serviceUtils.putNotEmpty(paramMap, COL.TASKS, tasks);
                serviceUtils.putNotEmpty(paramMap, COL.ACTIVE, active);

            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
            Query query = new Query(TAB.EVENT, false)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger, paramMap), paramMap);

            if (active) {
                if (eventName != null)
                    createDatabaseEvent(eventId, eventName, buckets, schedule, tasks);
                else
                    createDatabaseEvent(eventId, eName, buckets, schedule, tasks);
            }

        } else {
            throw new ItemDoNotExistsException("Event", eventId);
        }
    }

    public Map<ResultField, Object> getEventsLog(Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        Map<String, Object> paramMap = new HashMap<>();

        if (urlConditions != null)
            conditions.addAll(urlConditions);

        Query queryCount = new Query(TAB.EVENT_LOG, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.EVENT_LOG, true)
                .select(serviceUtils.getEventsLogColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<ResultField, Object> result = new HashMap<>();
        result.put(ResultField.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger, paramMap), paramMap, Long.TYPE));
        result.put(ResultField.DATA, jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap));

        return result;
    }

    private void deactivateEvents(List<Integer> eventsIds) throws UnknownColumnException, ConditionNotAllowedException {
        Condition condition = new Condition(COL.EVENT_ID, Operator.in, eventsIds);
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(COL.UPDATED_BY, "server");
        paramMap.put(COL.ACTIVE, false);

        Query query = new Query(TAB.EVENT, true)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        this.jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
    }

    public void deleteEvent(String userName, Integer eventId) throws DataAccessException, UnknownColumnException, ItemDoNotExistsException, ConditionNotAllowedException {
        if (serviceUtils.isItemExist(eventId)) {

            Map<String, Object> event = getEvent(eventId);
            String eName = (String) event.get(COL.EVENT_NAME);
            removeDatabaseEvent(eName);

            Condition condition = new Condition(COL.EVENT_ID, Operator.equal, eventId);

            Map<String, Object> namedParameters = new HashMap<>();
            namedParameters.put(COL.DELETED, true);
            namedParameters.put(COL.UPDATED_BY, userName);

            // Set event as deleted
            Query query = new Query(TAB.EVENT, true)
                    .update()
                    .set(namedParameters)
                    .where(condition, namedParameters);

            jdbcTemplate.update(query.toString(logger, namedParameters), namedParameters);
        } else
            throw new ItemDoNotExistsException("Event", eventId);
    }

    public void clearEventsLog() {
        Query query = new Query(TAB.EVENT_LOG, false)
                .delete()
                .from();

        jdbcTemplate.getJdbcTemplate().update(query.toString(logger, null));
    }

    private void createDatabaseEvent(int eventId, String eventName, List<Map<String, Object>> buckets, Map<String, Object> schedule, List<Map<String, Object>> tasks) throws UnknownColumnException, UnexpectedException, EmptyInputValueException, ConditionNotAllowedException {
        String eventQuery = "";

        eventQuery += "CREATE EVENT \"" + eventName + "\"\n";
        eventQuery += "\tON SCHEDULE\n";

        // Schedule
        boolean periodically = (boolean) schedule.get(C.PERIODICALLY);
        if (periodically) {
            String starts = serviceUtils.getFormattedDate((String) schedule.get(C.STARTS));
            Map<String, Object> interval = (Map<String, Object>) schedule.get(C.INTERVAL);
            int amount = (int) interval.get(C.AMOUNT);
            String unit = serviceUtils.getUnit((int) interval.get(C.UNIT));
            eventQuery += "\tEVERY " + amount + " " + unit + "\n";
            eventQuery += "\tSTARTS '" + starts + "'\n";

            boolean enabled_end = (boolean) schedule.get(C.ENABLE_ENDS);
            if (enabled_end) {
                String ends = serviceUtils.getFormattedDate((String) schedule.get(C.ENDS));
                eventQuery += "\tENDS '" + ends + "'\n";
            }
        } else {
            String at = serviceUtils.getFormattedDate((String) schedule.get(C.STARTS));
            eventQuery += "\tAT '" + at + "'\n";
        }

        eventQuery += "DO\n";
        eventQuery += "\tBEGIN\n";

        // Tasks
        for (Map<String, Object> tableTask : tasks) {
            int taskId = Integer.parseInt((String) tableTask.get("task_id"));
            Map<String, Object> task = serviceUtils.getTask(taskId);
            String taskName = (String) task.get(COL.TASK_NAME);
            Map<String, Object> configuration = (Map<String, Object>) task.get(C.CONFIGURATION);
            List<Condition> conditions = FieldValidator.validateListOfConditions(configuration, false);
            Map<String, Object> actions = (Map<String, Object>) configuration.get(C.ACTIONS);
            String actionType = (String) actions.get(C.TYPE);
            if (actionType.equals(C.MODIFY)) {
                for (Map<String, Object> bucket : buckets) {
                    int bucketId = (int) bucket.get(COL.BUCKET_ID);
                    String bucketName = (String) bucket.get(COL.BUCKET_NAME);
                    String modifyQuery = serviceUtils.getTaskModifyQuery(eventName, bucketName, conditions, actions);
                    eventQuery += "\t\t/* Task: '" + taskName + "' for bucket '" + bucketName + "' */\n";
                    eventQuery += "\t\t" + modifyQuery + ";\n";
                    eventQuery += "\t\tSELECT row_count() INTO @affected_rows;\n";
                    eventQuery += "\t\tINSERT INTO \"_event_log\" (\"event_id\", \"task_id\", \"bucket_id\", \"affected\") VALUES (" + eventId + ", " + taskId + ", " + bucketId + ", @affected_rows);\n\n";
                }
            } else if (actionType.equals(C.REMOVE)) {
                for (Map<String, Object> bucket : buckets) {
                    int bucketId = (int) bucket.get(COL.BUCKET_ID);
                    String bucketName = (String) bucket.get(COL.BUCKET_NAME);
                    String removeQuery = serviceUtils.getTaskRemoveQuery(bucketName, conditions);
                    eventQuery += "\t\t/* Task: '" + taskName + "' for bucket '" + bucketName + "' */\n";
                    eventQuery += "\t\t" + removeQuery + ";\n";
                    eventQuery += "\t\tSELECT row_count() INTO @affected_rows;\n";
                    eventQuery += "\t\tINSERT INTO \"_event_log\" (\"event_id\", \"task_id\", \"bucket_id\", \"affected\") VALUES (" + eventId + ", " + taskId + ", " + bucketId + ", @affected_rows);\n\n";
                }
            } else
                throw new UnexpectedException("Undefined action type. Expected one of the [modify, remove]. Given " + actionType);
        }

        eventQuery += "\tEND\n";

        logger.debug(eventQuery);
        jdbcTemplate.getJdbcTemplate().execute(eventQuery);

//		enableEventScheduler();
    }

    private void removeDatabaseEvent(String eventName) throws DataAccessException {
        String query = "DROP EVENT IF EXISTS \"" + eventName + "\"";
        jdbcTemplate.getJdbcTemplate().execute(query);
        logger.debug(query);
    }

}
