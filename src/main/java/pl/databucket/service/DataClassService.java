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
public class DataClassService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ServiceUtils serviceUtils;
    Logger logger = LoggerFactory.getLogger(DataClassService.class);

    public DataClassService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.serviceUtils = new ServiceUtils(jdbcTemplate, logger);
    }

    public int createClass(String userName, String className, String description) throws ClassAlreadyExistsException, ConditionNotAllowedException, UnknownColumnException, JsonProcessingException, SQLException {
        if (serviceUtils.isClassExist(className))
            throw new ClassAlreadyExistsException(className);

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue(COL.CLASS_NAME, className);
        namedParameters.addValue(COL.CREATED_BY, userName);
        serviceUtils.putNotEmpty(namedParameters, COL.DESCRIPTION, description);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(TAB.CLASS, true).insertIntoValues(namedParameters);
        jdbcTemplate.update(query.toString(logger, null), namedParameters, keyHolder, new String[]{COL.CLASS_ID});
        return keyHolder.getKey().intValue();
    }

    public void deleteClass(Integer classId, String userName) throws ItemDoNotExistsException, UnknownColumnException, ItemAlreadyUsedException, ConditionNotAllowedException {
        if (!serviceUtils.isClassExist(classId))
            throw new ItemDoNotExistsException("Class", classId);

        String message = referencesClassItem(classId);
        if (message != null)
            throw new ItemAlreadyUsedException(message);

        Condition condition = new Condition(COL.CLASS_ID, Operator.equal, classId);

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Set class as deleted
        Query query = new Query(TAB.CLASS, false)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
    }

    private String referencesClassItem(int classId) throws UnknownColumnException, ConditionNotAllowedException {
        final String AS_ID = " as \"id\"";
        final String AS_NAME = " as \"name\"";
        String result = "";

        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.CLASS_ID, Operator.equal, classId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.BUCKET, true)
                .select(new String[]{COL.BUCKET_ID + AS_ID, COL.BUCKET_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
        result += serviceUtils.getStringWithItemsNames(C.BUCKETS, resultList);

        query = new Query(TAB.TAG, true)
                .select(new String[]{COL.TAG_ID + AS_ID, COL.TAG_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
        result += serviceUtils.getStringWithItemsNames(C.TAGS, resultList);

        query = new Query(TAB.COLUMNS, true)
                .select(new String[]{COL.COLUMNS_ID + AS_ID, COL.COLUMNS_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
        result += serviceUtils.getStringWithItemsNames(C.COLUMNS, resultList);

        query = new Query(TAB.FILTER, true)
                .select(new String[]{COL.FILTER_ID + AS_ID, COL.FILTER_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
        result += serviceUtils.getStringWithItemsNames(C.FILTERS, resultList);

        query = new Query(TAB.TASK, true)
                .select(new String[]{COL.TASK_ID + AS_ID, COL.TASK_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
        result += serviceUtils.getStringWithItemsNames(C.TASKS, resultList);

        query = new Query(TAB.VIEW, true)
                .select(new String[]{COL.VIEW_ID + AS_ID, COL.VIEW_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
        result += serviceUtils.getStringWithItemsNames(C.VIEWS, resultList);

        query = new Query(TAB.EVENT, true)
                .select(new String[]{COL.EVENT_ID + AS_ID, COL.EVENT_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
        result += serviceUtils.getStringWithItemsNames(C.EVENTS, resultList);

        if (result.length() > 0)
            return result;
        else
            return null;
    }

    public Map<ResultField, Object> getClasses(Optional<Integer> classId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException, DataAccessException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        Map<String, Object> paramMap = new HashMap<>();

        if (classId.isPresent()) {
            conditions.add(new Condition(COL.CLASS_ID, Operator.equal, classId.get()));
        }

        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (urlConditions != null)
            conditions.addAll(urlConditions);

        Query queryCount = new Query(TAB.CLASS, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.CLASS, true)
                .select(serviceUtils.getClassColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<ResultField, Object> result = new HashMap<>();
        result.put(ResultField.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger, paramMap), paramMap, Long.TYPE));
        result.put(ResultField.DATA, jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap));

        return result;
    }

    public void modifyClass(String userName, Integer classId, LinkedHashMap<String, Object> body) throws ItemDoNotExistsException, ClassAlreadyExistsException, JsonProcessingException, DataAccessException, UnknownColumnException, SQLException, ConditionNotAllowedException {
        if (serviceUtils.isClassExist(classId)) {

            if (body.containsKey(COL.CLASS_NAME)) {
                String newClassName = (String) body.get(COL.CLASS_NAME);

                if (serviceUtils.isClassExist(classId, newClassName))
                    throw new ClassAlreadyExistsException(newClassName);
            }

            Condition condition = new Condition(COL.CLASS_ID, Operator.equal, classId);
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put(COL.UPDATED_BY, userName);
            serviceUtils.putNotEmpty(body, paramMap, COL.CLASS_NAME);
            serviceUtils.putNotEmpty(body, paramMap, COL.DESCRIPTION);

            Query query = new Query(TAB.CLASS, false)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger, paramMap), paramMap);

        } else {
            throw new ItemDoNotExistsException("Class", classId);
        }
    }



}
