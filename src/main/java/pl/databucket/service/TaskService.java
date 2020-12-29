package pl.databucket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class TaskService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ServiceUtils serviceUtils;
    Logger logger = LoggerFactory.getLogger(TaskService.class);

    public TaskService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.serviceUtils = new ServiceUtils(jdbcTemplate, logger);
    }

    public int createTask(String taskName, Integer bucketId, Integer classId, String createdBy, String description, Map<String, Object> configuration) throws JsonProcessingException, SQLException {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue(COL.TASK_NAME, taskName);
        namedParameters.addValue(COL.CREATED_BY, createdBy);
        namedParameters.addValue(COL.CONFIGURATION, serviceUtils.javaObjectToPGObject(configuration));
        serviceUtils.putNotEmpty(namedParameters, COL.DESCRIPTION, description);
        serviceUtils.putNotEmpty(namedParameters, COL.CLASS_ID, classId);
        serviceUtils.putNotEmpty(namedParameters, COL.BUCKET_ID, bucketId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(TAB.TASK, true).insertIntoValues(namedParameters);
        jdbcTemplate.update(query.toString(logger, null), namedParameters, keyHolder, new String[]{COL.TASK_ID});
        return keyHolder.getKey().intValue();
    }

    public Map<String, Object> getTasks(Optional<String> bucketName, Optional<Integer> taskId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnknownColumnException, UnexpectedException, ConditionNotAllowedException {
        Map<String, Object> paramMap = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (taskId.isPresent()) {
            conditions.add(new Condition(COL.TASK_ID, Operator.equal, taskId.get()));
        } else if (bucketName.isPresent()) {
            int bucketId = serviceUtils.getBucketId(bucketName.get());
            conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));
        }

        if (urlConditions != null)
            conditions.addAll(urlConditions);

        Query queryCount = new Query(TAB.TASK, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.TASK, true)
                .select(serviceUtils.getTaskColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<String, Object> result = new HashMap<>();
        result.put(C.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger, paramMap), paramMap, Long.TYPE));
        List<Map<String, Object>> taskList = jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap);
        serviceUtils.convertStringToMap(taskList, COL.CONFIGURATION);
        result.put(C.TASKS, taskList);

        return result;
    }

    public void modifyTask(String updatedBy, Integer taskId, String taskName, Integer bucketId, Integer classId, String description, LinkedHashMap<String, Object> configuration) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException, ConditionNotAllowedException {
        if (serviceUtils.isTaskExist(taskId)) {
            Condition condition = new Condition(COL.TASK_ID, Operator.equal, taskId);
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put(COL.UPDATED_BY, updatedBy);
            try {
                serviceUtils.putNotEmptyNullableInteger(paramMap, COL.BUCKET_ID, bucketId);
                serviceUtils.putNotEmptyNullableInteger(paramMap, COL.CLASS_ID, classId);
                serviceUtils.putNotEmpty(paramMap, COL.TASK_NAME, taskName);
                serviceUtils.putNotEmpty(paramMap, COL.DESCRIPTION, description);
                serviceUtils.putNotEmpty(paramMap, COL.CONFIGURATION, configuration);

            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
            Query query = new Query(TAB.TASK, false)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
        } else {
            throw new ItemDoNotExistsException("Task", taskId);
        }
    }

    public void deleteTask(String userName, int taskId) throws ItemDoNotExistsException, UnknownColumnException, ItemAlreadyUsedException, ConditionNotAllowedException {
        if (serviceUtils.isTaskExist(taskId)) {

            String message = referencesTaskItem(taskId);
            if (message != null)
                throw new ItemAlreadyUsedException(message);

            Condition condition = new Condition(COL.TASK_ID, Operator.equal, taskId);

            Map<String, Object> namedParameters = new HashMap<>();
            namedParameters.put(COL.DELETED, true);
            namedParameters.put(COL.UPDATED_BY, userName);

            // Set task as deleted
            Query query = new Query(TAB.TASK, true)
                    .update()
                    .set(namedParameters)
                    .where(condition, namedParameters);

            jdbcTemplate.update(query.toString(logger, namedParameters), namedParameters);
        } else
            throw new ItemDoNotExistsException("Task", taskId);
    }

    private String referencesTaskItem(int taskId) throws UnknownColumnException, ConditionNotAllowedException {
        final String AS_ID = " as 'id'";
        final String AS_NAME = " as 'name'";
        String result = "";

        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.EVENT, true)
                .select(new String[]{COL.EVENT_ID + AS_ID, COL.EVENT_NAME + AS_NAME, COL.TASKS})
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> eventList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);

        if (eventList.size() > 0) {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> refEventList = new ArrayList<>();
            for (Map<String, Object> event: eventList) {
                String eventTasksStr = (String) event.get(COL.TASKS);
                try {
                    List<Map<String, Object>> eventTasks = mapper.readValue(eventTasksStr, new TypeReference<List<Map<String, Object>>>() {});
                    if (eventTasks.size() > 0) {
                        for (Map<String, Object> eventTask: eventTasks) {
                            int eventTaskId = Integer.parseInt((String) eventTask.get(COL.TASK_ID));
                            if (eventTaskId == taskId)
                                refEventList.add(event);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (refEventList.size() > 0) {
                result += serviceUtils.getStringWithItemsNames(C.EVENTS, refEventList);
            }
        }

        conditions = new ArrayList<>();
        conditions.add(new Condition(COL.TASK_ID, Operator.equal, taskId));
        query = new Query(TAB.EVENT_LOG, false)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);
        int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);
        if (count > 1)
            result += "\nLOGS";

        if (result.length() > 0)
            return result;
        else
            return null;
    }

}
