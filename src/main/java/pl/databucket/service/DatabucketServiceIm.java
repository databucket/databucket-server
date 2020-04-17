package pl.databucket.service;

import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.databucket.exception.*;
import pl.databucket.database.C;
import pl.databucket.database.COL;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.database.Operator;
import pl.databucket.database.Query;
import pl.databucket.database.TAB;

@Service
public class DatabucketServiceIm implements DatabucketService {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    Logger logger = LoggerFactory.getLogger(DatabucketServiceIm.class);


    public int createGroup(String userName, String groupName, String description, ArrayList<Integer> buckets) throws GroupAlreadyExistsException, ExceededMaximumNumberOfCharactersException, EmptyInputValueException, Exception {
        if (isGroupExist(groupName))
            throw new GroupAlreadyExistsException(groupName);

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue(COL.GROUP_NAME, groupName);
        namedParameters.addValue(COL.CREATED_BY, userName);
        putNotEmpty(namedParameters, COL.DESCRIPTION, description);
        putNotEmpty(namedParameters, COL.BUCKETS, buckets);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(TAB.GROUP, true).insertIntoValues(namedParameters);
        jdbcTemplate.update(query.toString(logger), namedParameters, keyHolder, new String[]{COL.GROUP_ID});
        return keyHolder.getKey().intValue();
    }

    ;

    public int deleteGroup(Integer groupId, String userName) throws ItemDoNotExistsException, UnknownColumnException {
        Condition condition = new Condition(COL.GROUP_ID, Operator.equal, groupId);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Set group as deleted
        Query query = new Query(TAB.GROUP, true)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        return jdbcTemplate.update(query.toString(logger), paramMap);
    }

    ;

    public Map<String, Object> getGroups(Optional<Integer> groupId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException, DataAccessException, UnexpectedException {
        List<Condition> conditions = new ArrayList<Condition>();
        Map<String, Object> paramMap = new HashMap<String, Object>();

        if (groupId.isPresent()) {
            conditions.add(new Condition(COL.GROUP_ID, Operator.equal, groupId.get()));
        }

        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (urlConditions != null)
            for (Condition condition : urlConditions)
                conditions.add(condition);

        Query queryCount = new Query(TAB.GROUP, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.GROUP, true)
                .select(getGroupColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(C.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger), paramMap, Long.TYPE));
        List<Map<String, Object>> groupList = jdbcTemplate.queryForList(queryData.toString(logger), paramMap);
        convertStringToArray(groupList, COL.BUCKETS);
        result.put(C.GROUPS, groupList);

        return result;
    }

    ;


    public void modifyGroup(String userName, Integer groupId, LinkedHashMap<String, Object> body) throws ItemDoNotExistsException, GroupAlreadyExistsException, JsonProcessingException, IncorrectValueException, ExceededMaximumNumberOfCharactersException, DataAccessException, UnknownColumnException {
        if (isGroupExist(groupId)) {

            if (body.containsKey(COL.GROUP_NAME)) {
                String newGroupName = (String) body.get(COL.GROUP_NAME);

                if (isGroupExist(groupId, newGroupName))
                    throw new GroupAlreadyExistsException(newGroupName);
            }

            Condition condition = new Condition(COL.GROUP_ID, Operator.equal, groupId);
            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put(COL.UPDATED_BY, userName);
            putNotEmpty(body, paramMap, COL.GROUP_NAME);
            putNotEmpty(body, paramMap, COL.DESCRIPTION);
            putNotEmpty(body, paramMap, COL.BUCKETS);

            Query query = new Query(TAB.GROUP, true)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger), paramMap);

        } else {
            throw new ItemDoNotExistsException("Group", groupId);
        }
    }

    ;

    public int createClass(String userName, String className, String description) throws ClassAlreadyExistsException, ExceededMaximumNumberOfCharactersException, EmptyInputValueException, Exception {
        if (isClassExist(className))
            throw new ClassAlreadyExistsException(className);

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue(COL.CLASS_NAME, className);
        namedParameters.addValue(COL.CREATED_BY, userName);
        putNotEmpty(namedParameters, COL.DESCRIPTION, description);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(TAB.CLASS, true).insertIntoValues(namedParameters);
        jdbcTemplate.update(query.toString(logger), namedParameters, keyHolder, new String[]{COL.CLASS_ID});
        return keyHolder.getKey().intValue();
    }

    ;

    public int deleteClass(Integer classId, String userName) throws ItemDoNotExistsException, UnknownColumnException, ItemAlreadyUsedException {
        if (!isClassExist(classId))
            throw new ItemDoNotExistsException("Class", classId);

        String message = referencesClassItem(classId);
        if (message != null)
            throw new ItemAlreadyUsedException(message);

        Condition condition = new Condition(COL.CLASS_ID, Operator.equal, classId);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Set class as deleted
        Query query = new Query(TAB.CLASS, true)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        return jdbcTemplate.update(query.toString(logger), paramMap);
    }

    ;

    private String referencesClassItem(int classId) throws UnknownColumnException {
        final String AS_ID = " as 'id'";
        final String AS_NAME = " as 'name'";
        String result = "";

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.CLASS_ID, Operator.equal, classId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.BUCKET, true)
                .select(new String[]{COL.BUCKET_ID + AS_ID, COL.BUCKET_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.BUCKETS, resultList);

        query = new Query(TAB.TAG, true)
                .select(new String[]{COL.TAG_ID + AS_ID, COL.TAG_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.TAGS, resultList);

        query = new Query(TAB.COLUMNS, true)
                .select(new String[]{COL.COLUMNS_ID + AS_ID, COL.COLUMNS_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.COLUMNS, resultList);

        query = new Query(TAB.FILTER, true)
                .select(new String[]{COL.FILTER_ID + AS_ID, COL.FILTER_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.FILTERS, resultList);

        query = new Query(TAB.TASK, true)
                .select(new String[]{COL.TASK_ID + AS_ID, COL.TASK_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.TASKS, resultList);

        query = new Query(TAB.VIEW, true)
                .select(new String[]{COL.VIEW_ID + AS_ID, COL.VIEW_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.VIEWS, resultList);

        query = new Query(TAB.EVENT, true)
                .select(new String[]{COL.EVENT_ID + AS_ID, COL.EVENT_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.EVENTS, resultList);

        if (result.length() > 0)
            return result;
        else
            return null;
    }

    public Map<String, Object> getClasses(Optional<Integer> classId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException, DataAccessException, UnexpectedException {
        List<Condition> conditions = new ArrayList<Condition>();
        Map<String, Object> paramMap = new HashMap<String, Object>();

        if (classId.isPresent()) {
            conditions.add(new Condition(COL.CLASS_ID, Operator.equal, classId.get()));
        }

        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (urlConditions != null)
            for (Condition condition : urlConditions)
                conditions.add(condition);

        Query queryCount = new Query(TAB.CLASS, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.CLASS, true)
                .select(getClassColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(C.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger), paramMap, Long.TYPE));
        result.put(C.CLASSES, jdbcTemplate.queryForList(queryData.toString(logger), paramMap));

        return result;
    }

    ;


    public void modifyClass(String userName, Integer classId, LinkedHashMap<String, Object> body) throws ItemDoNotExistsException, ClassAlreadyExistsException, JsonProcessingException, IncorrectValueException, ExceededMaximumNumberOfCharactersException, DataAccessException, UnknownColumnException {
        if (isClassExist(classId)) {

            if (body.containsKey(COL.CLASS_NAME)) {
                String newClassName = (String) body.get(COL.CLASS_NAME);

                if (isClassExist(classId, newClassName))
                    throw new ClassAlreadyExistsException(newClassName);
            }

            Condition condition = new Condition(COL.CLASS_ID, Operator.equal, classId);
            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put(COL.UPDATED_BY, userName);
            putNotEmpty(body, paramMap, COL.CLASS_NAME);
            putNotEmpty(body, paramMap, COL.DESCRIPTION);

            Query query = new Query(TAB.CLASS, true)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger), paramMap);

        } else {
            throw new ItemDoNotExistsException("Class", classId);
        }
    }

    ;

    private List<Map<String, Object>> getBuckets(Integer bucketId, Integer classId) throws UnknownColumnException {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (bucketId != null && bucketId != -1)
            conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));

        if (classId != null && classId != -1)
            conditions.add(new Condition(COL.CLASS_ID, Operator.equal, classId));

        Query queryData = new Query(TAB.BUCKET, true)
                .select(getBucketColumns())
                .from()
                .where(conditions, paramMap);

        return jdbcTemplate.queryForList(queryData.toString(logger), paramMap);
    }

    public Map<String, Object> getBuckets(Optional<String> bucketName, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException, DataAccessException {

        Map<String, Object> paramMap = new HashMap<String, Object>();
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (bucketName.isPresent())
            conditions.add(new Condition(COL.BUCKET_NAME, Operator.equal, bucketName.get()));

        if (urlConditions != null)
            for (Condition condition : urlConditions)
                conditions.add(condition);

        Query queryCount = new Query(TAB.BUCKET, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.BUCKET, true)
                .select(getBucketColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(C.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger), paramMap, Long.TYPE));
        result.put(C.BUCKETS, jdbcTemplate.queryForList(queryData.toString(logger), paramMap));

        return result;
    }

    public List<Map<String, Object>> getStatistic(String bucketName) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException {
        String[] columns = {COL.TAG_ID, COL.TAG_NAME, COL.LOCKED, COL.COUNT};
        String[] columns2 = {COL.TAG_ID, COL.LOCKED};
        Map<String, Object> paramMap = new HashMap<String, Object>();
        Query query = new Query(bucketName, true)
                .select(columns)
                .from()
                .leftOuterJoin(TAB.TAG, "t", COL.TAG_ID, "a", COL.TAG_ID)
                .groupBy(columns2);

        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(query.toString(logger), paramMap);
            return convertBucketInfo(result);
        } catch (BadSqlGrammarException e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }

    public int createBucket(String createdBy, String bucketName, int index, String description, String icon, boolean history, Integer classId) throws BucketAlreadyExistsException, Exception {
        if (!isBucketExist(bucketName)) {

            MapSqlParameterSource namedParameters = new MapSqlParameterSource();
            namedParameters.addValue(COL.BUCKET_NAME, bucketName);
            namedParameters.addValue(COL.INDEX, index);
            namedParameters.addValue(COL.ICON_NAME, icon);
            namedParameters.addValue(COL.CREATED_BY, createdBy);
            namedParameters.addValue(COL.HISTORY, history);
            namedParameters.addValue(COL.CLASS_ID, classId);
            if (description != null)
                namedParameters.addValue(COL.DESCRIPTION, description);

            KeyHolder keyHolder = new GeneratedKeyHolder();

            Query query = new Query(TAB.BUCKET, true).insertIntoValues(namedParameters);

            jdbcTemplate.update(query.toString(logger), namedParameters, keyHolder, new String[]{COL.BUCKET_ID});
            int bucketId = keyHolder.getKey().intValue();

            // Create table for data
            String sql = "CREATE TABLE `" + bucketName + "` ("
                    + "`bundle_id` INT(11) NOT NULL AUTO_INCREMENT,"
                    + "`tag_id` INT(5) NOT NULL DEFAULT 0,"
                    + "`locked` BIT(1) NOT NULL DEFAULT b'0',"
                    + "`locked_by` VARCHAR(45) NULL DEFAULT NULL,"
                    + "`properties` JSON DEFAULT NULL,"
                    + "`created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "`created_by` VARCHAR(45) NULL DEFAULT NULL,"
                    + "`updated_at` TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,"
                    + "`updated_by` VARCHAR(45) NULL DEFAULT NULL,"

                    + "PRIMARY KEY (`bundle_id`)," + "KEY `tag_id_idx` (`tag_id`)," + "INDEX `locked_idx` (`locked` ASC),"
                    + "CONSTRAINT `" + bucketName + "_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `_tag` (`tag_id`) ON DELETE NO ACTION ON UPDATE NO ACTION) "
                    + "AUTO_INCREMENT = 1 " + "DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;";

            logger.debug(sql);
            jdbcTemplate.getJdbcTemplate().execute(sql);

            // Create table for history
            sql = "CREATE TABLE `" + bucketName + "_history` ("
                    + "`id` int(11) NOT NULL AUTO_INCREMENT,"
                    + "`bundle_id` int(11) NOT NULL,"
                    + "`tag_id` int(5) DEFAULT NULL,"
                    + "`locked` bit(1) DEFAULT NULL,"
                    + "`properties` json DEFAULT NULL,"
                    + "`updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "`updated_by` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,"
                    + "PRIMARY KEY (`id`),"
                    + "UNIQUE KEY `id_UNIQUE` (`id`),"
                    + "KEY `fk_" + bucketName + "_bundle_id` (`bundle_id`),"
                    + "CONSTRAINT `fk_" + bucketName + "_bundle_id` FOREIGN KEY (`bundle_id`) REFERENCES `" + bucketName + "` (`bundle_id`) ON DELETE NO ACTION ON UPDATE NO ACTION"
                    + ") ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

            logger.debug(sql);
            jdbcTemplate.getJdbcTemplate().execute(sql);

//			createBeforeUpdateTrigger(bucketName); // now we update it by 
            createBeforeDeleteTrigger(bucketName);

            // Create after insert and after update triggers if history is enabled
            if (history == true) {
                createAfterInsertTrigger(bucketName);
                createAfterUpdateTrigger(bucketName);
            }

            return bucketId;
        } else {
            throw new BucketAlreadyExistsException(bucketName);
        }
    }

    public void deleteBucket(String bucketName, String userName) throws ItemDoNotExistsException, UnknownColumnException, ItemAlreadyUsedException {
        if (!isBucketExist(bucketName))
            throw new ItemDoNotExistsException("Bucket", bucketName);

        int bucketId = getBucketId(bucketName);

        String message = referencesBucketItem(bucketId);
        if (message != null)
            throw new ItemAlreadyUsedException(message);

        Condition condition = new Condition(COL.BUCKET_ID, Operator.equal, bucketId);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Set bucket as deleted
        Query query = new Query(TAB.BUCKET, true)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        paramMap.put(COL.BUCKET_ID, bucketId);
        jdbcTemplate.update(query.toString(logger), paramMap);

        paramMap = new HashMap<String, Object>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Delete tags from _tags
        query = new Query(TAB.TAG, false)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        paramMap.put(COL.BUCKET_ID, bucketId);
        jdbcTemplate.update(query.toString(logger), paramMap);

        paramMap = new HashMap<String, Object>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Delete filters from _filters
        query = new Query(TAB.FILTER, false)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        paramMap.put(COL.BUCKET_ID, bucketId);
        jdbcTemplate.update(query.toString(logger), paramMap);

        paramMap = new HashMap<String, Object>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Delete views from _views
        query = new Query(TAB.VIEW, false)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        paramMap.put(COL.BUCKET_ID, bucketId);
        jdbcTemplate.update(query.toString(logger), paramMap);

        // Drop bucket history table
        query = new Query(bucketName + "_history", false)
                .dropTable();

        jdbcTemplate.getJdbcTemplate().execute(query.toString(logger));

        // Drop bucket table
        query = new Query(bucketName, false)
                .dropTable();

        jdbcTemplate.getJdbcTemplate().execute(query.toString(logger));
    }

    private String referencesBucketItem(int bucketId) throws UnknownColumnException {
        final String AS_ID = " as 'id'";
        final String AS_NAME = " as 'name'";
        String result = "";

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.TAG, true)
                .select(new String[]{COL.TAG_ID + AS_ID, COL.TAG_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.TAGS, resultList);

        query = new Query(TAB.COLUMNS, true)
                .select(new String[]{COL.COLUMNS_ID + AS_ID, COL.COLUMNS_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.COLUMNS, resultList);

        query = new Query(TAB.FILTER, true)
                .select(new String[]{COL.FILTER_ID + AS_ID, COL.FILTER_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.FILTERS, resultList);

        query = new Query(TAB.TASK, true)
                .select(new String[]{COL.TASK_ID + AS_ID, COL.TASK_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.TASKS, resultList);

        query = new Query(TAB.VIEW, true)
                .select(new String[]{COL.VIEW_ID + AS_ID, COL.VIEW_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.VIEWS, resultList);

        query = new Query(TAB.EVENT, true)
                .select(new String[]{COL.EVENT_ID + AS_ID, COL.EVENT_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.EVENTS, resultList);

        if (result.length() > 0)
            return result;
        else
            return null;
    }

    private String getStringWithItemsNames(String itemType, List<Map<String, Object>> objects) {
        StringBuilder result = new StringBuilder();
        if (objects.size() > 0)
            for (Map<String, Object> obj : objects)
                result.append(String.format(" [%s] %s\n", obj.get("id"), obj.get("name")));

        if (result.length() > 0)
            return "\n" + itemType.toUpperCase() + ":\n" + result.toString();
        else
            return "";
    }


    @SuppressWarnings("unchecked")
    public void modifyBucket(String updatedBy, String bucketName, Map<String, Object> details) throws ItemDoNotExistsException, BucketAlreadyExistsException, JsonProcessingException, IncorrectValueException, ExceededMaximumNumberOfCharactersException, DataAccessException, UnknownColumnException {
        Map<String, Object> result = getBuckets(Optional.of(bucketName), Optional.empty(), Optional.empty(), Optional.empty(), null);
        List<Map<String, Object>> buckets = (List<Map<String, Object>>) result.get(C.BUCKETS);

        if (buckets.size() > 0) {
            Map<String, Object> bucket = buckets.get(0);

            if (details.containsKey(COL.BUCKET_NAME)) {
                String newBucketName = (String) details.get(COL.BUCKET_NAME);
                if (newBucketName != null) {
                    String currentBucketName = (String) bucket.get(COL.BUCKET_NAME);
                    if (!newBucketName.equals(currentBucketName)) {
                        if (!newBucketName.toLowerCase().equals(bucketName.toLowerCase())) {
                            if (!isBucketExist(newBucketName)) {
                                modifyBucketName(bucketName, newBucketName);
                            } else {
                                throw new BucketAlreadyExistsException(newBucketName);
                            }
                        }
                    }
                } else
                    throw new IncorrectValueException("Bucket name can not be empty!");

                bucketName = newBucketName;
            }

            if (details.containsKey(COL.DESCRIPTION)) {
                String description = (String) details.get(COL.DESCRIPTION);
                if (description.length() > 100)
                    throw new ExceededMaximumNumberOfCharactersException(COL.DESCRIPTION, description, 100);
            }

            Condition conBucketId = new Condition(COL.BUCKET_ID, Operator.equal, bucket.get(COL.BUCKET_ID));

            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put(COL.UPDATED_BY, updatedBy);
            putNotEmpty(details, paramMap, COL.BUCKET_NAME);
            putNotEmpty(details, paramMap, COL.DESCRIPTION);
            putNotEmpty(details, paramMap, COL.INDEX);
            putNotEmpty(details, paramMap, COL.ICON_NAME);
            putNotEmpty(details, paramMap, COL.HISTORY);

            if (details.containsKey(COL.CLASS_ID)) {
                Integer classId = (Integer) details.get(COL.CLASS_ID);
                paramMap.put(COL.CLASS_ID, classId);
            }

            Query query = new Query(TAB.BUCKET, true)
                    .update()
                    .set(paramMap)
                    .where(conBucketId, paramMap);

            this.jdbcTemplate.update(query.toString(logger), paramMap);

            if (details.get(COL.HISTORY) != null) {
                boolean enableHistory = (boolean) details.get(COL.HISTORY);
                if (enableHistory) {
                    createAfterInsertTrigger(bucketName);
                    createAfterUpdateTrigger(bucketName);
                } else {
                    removeAfterInsertTrigger(bucketName);
                    removeAfterUpdateTrigger(bucketName);
                }
            }
        } else {
            throw new ItemDoNotExistsException("Bucket", bucketName);
        }
    }

    private void modifyBucketName(String bucketName, String newBucketName) throws UnknownColumnException {
        // Remove item with last name
        removeBeforeUpdateTrigger(bucketName);
        removeBeforeDeleteTrigger(bucketName);
        removeAfterInsertTrigger(bucketName);
        removeAfterUpdateTrigger(bucketName);

        String sql = "ALTER TABLE `" + bucketName + "` DROP FOREIGN KEY `" + bucketName + "_tag_id`";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);

        sql = "ALTER TABLE `" + bucketName + "_history` DROP FOREIGN KEY `fk_" + bucketName + "_bundle_id`";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);

        sql = "ALTER TABLE `" + bucketName + "_history` DROP INDEX `fk_" + bucketName + "_bundle_id`";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);

        // Change table names
        sql = "ALTER TABLE `" + bucketName + "` RENAME TO `" + newBucketName + "`";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);

        sql = "ALTER TABLE `" + bucketName + "_history` RENAME TO `" + newBucketName + "_history`";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);

        // Create items with new name
        sql = "ALTER TABLE `" + newBucketName + "` ADD CONSTRAINT `" + newBucketName + "_tag_id` FOREIGN KEY (`tag_id`) REFERENCES `_tag` (`tag_id`) ON DELETE NO ACTION ON UPDATE NO ACTION";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);

        sql = "ALTER TABLE `" + newBucketName + "_history` ADD CONSTRAINT `fk_" + newBucketName + "_bundle_id` FOREIGN KEY (`bundle_id`) REFERENCES `" + newBucketName + "` (`bundle_id`) ON DELETE NO ACTION ON UPDATE NO ACTION";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);

        createBeforeDeleteTrigger(newBucketName);

        if (isHistoryEnabled(bucketName)) {
            createAfterInsertTrigger(newBucketName);
            createAfterUpdateTrigger(newBucketName);
        }
    }

    private void createBeforeDeleteTrigger(String bucketName) {
        removeBeforeDeleteTrigger(bucketName);

        String sql = "\nCREATE TRIGGER `" + bucketName + "_BEFORE_DELETE` BEFORE DELETE ON `" + bucketName + "` FOR EACH ROW\n" +
                "BEGIN\n" +
                "	DELETE FROM `" + bucketName + "_history` WHERE bundle_id = OLD.bundle_id;\n" +
                "END";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void createAfterInsertTrigger(String bucketName) {
        removeAfterInsertTrigger(bucketName);

        String sql = "\nCREATE TRIGGER `" + bucketName + "_AFTER_INSERT` AFTER INSERT ON `" + bucketName + "` FOR EACH ROW\n" +
                "BEGIN\n" +
                "    INSERT INTO `" + bucketName + "_history` (bundle_id, tag_id, locked, properties, updated_by)\n" +
                "			VALUES (NEW.bundle_id, NEW.tag_id, NEW.locked, NEW.properties, NEW.created_by);\n" +
                "END";

        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void createAfterUpdateTrigger(String bucketName) {
        removeAfterUpdateTrigger(bucketName);

        // Create after updated trigger
        String sql = "\nCREATE TRIGGER `" + bucketName + "_AFTER_UPDATE` AFTER UPDATE ON `" + bucketName + "` FOR EACH ROW\n" +
                "BEGIN\n" +
                "    SET @insert_changes = false;\n" +
                "    SET @tagId = null;\n" +
                "	IF ((OLD.tag_id is null) != (NEW.tag_id is null)) OR (OLD.tag_id != NEW.tag_id)  THEN\n" +
                "		SET @tagId = NEW.tag_id;\n" +
                "        SET @insert_changes = true;\n" +
                "    END IF;\n" +
                "    \n" +
                "    SET @locked = null;\n" +
                "    IF OLD.locked != NEW.locked THEN\n" +
                "		SET @locked = NEW.locked;\n" +
                "        SET @insert_changes = true;\n" +
                "    END IF;\n" +
                "    \n" +
                "    SET @properties = null;\n" +
                "    IF NEW.properties is null THEN\n" +
                "	    IF OLD.properties is not null THEN\n" +
                "		   SET @properties = '{\"---null---\":\"---null---\"}';\n" +
                "		   SET @insert_changes = true;\n" +
                "	    END IF;\n" +
                "    ELSE\n" +
                "	    IF (OLD.properties is null) OR (OLD.properties != NEW.properties) THEN\n" +
                "		   SET @properties = NEW.properties;\n" +
                "		   SET @insert_changes = true;\n" +
                "       END IF;\n" +
                "    END IF;\n" +
                "    \n" +
                "    IF @insert_changes = true THEN\n" +
                "		INSERT INTO `" + bucketName + "_history` (bundle_id, tag_id, locked, properties, updated_by)\n" +
                "			VALUES (NEW.bundle_id, @tagId, @locked, @properties, NEW.updated_by);\n" +
                "	END IF;\n" +
                "END";

        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void removeBeforeUpdateTrigger(String bucketName) {
        String sql = "DROP TRIGGER IF EXISTS `" + bucketName + "_BEFORE_UPDATE`";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void removeBeforeDeleteTrigger(String bucketName) {
        String sql = "DROP TRIGGER IF EXISTS `" + bucketName + "_BEFORE_DELETE`";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void removeAfterInsertTrigger(String bucketName) {
        String sql = "DROP TRIGGER IF EXISTS `" + bucketName + "_AFTER_INSERT`";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void removeAfterUpdateTrigger(String bucketName) {
        String sql = "DROP TRIGGER IF EXISTS `" + bucketName + "_AFTER_UPDATE`";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    public long getBundlesCount(String bucketName, Query query, Map<String, Object> namedParameters) throws ItemDoNotExistsException, UnexpectedException {
        try {
            return jdbcTemplate.queryForObject(query.toString(logger), namedParameters, Long.TYPE);
        } catch (Exception e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }

    public List<Integer> lockBundles(String bucketName, String userName, Optional<Integer[]> tagId, Optional<Integer> filterId, Optional<List<Condition>> conditions, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort) throws JsonParseException, JsonMappingException, IOException, ItemDoNotExistsException, UnexpectedException, UnknownColumnException {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        List<Condition> selectConditions = new ArrayList<Condition>();

        if (filterId.isPresent()) {
            selectConditions = getFilterConditions(filterId.get());
        } else if (tagId.isPresent()) {
            selectConditions.add(new Condition(COL.TAG_ID, Operator.in, new ArrayList<Integer>(Arrays.asList(tagId.get()))));
        } else if (conditions.isPresent()) {
            selectConditions = conditions.get();

        }

        boolean joinTags = false;
        if (sort.isPresent())
            joinTags = sort.get().contains(COL.TAG_NAME);
        joinTags = joinTags || usedTagNameColumn(selectConditions);

        selectConditions.add(new Condition(COL.LOCKED, Operator.equal, false));

        Query selectQuery = new Query(bucketName, true)
                .select(COL.BUNDLE_ID)
                .from()
                .joinTags(joinTags)
                .where(selectConditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        boolean done = false;
        int tryCount = 5;
        List<Integer> bundlesIdsList = null;

        while (!done && tryCount > 0) {
            try {
                bundlesIdsList = transactionTemplate.execute(new TransactionCallback<List<Integer>>() {
                    public List<Integer> doInTransaction(TransactionStatus paramTransactionStatus) {
                        List<Integer> listOfIds = jdbcTemplate.queryForList(selectQuery.toString(logger), paramMap, Integer.class);
                        if (listOfIds.size() > 0) {
                            Condition condition = new Condition(COL.BUNDLE_ID, Operator.in, listOfIds);

                            Map<String, Object> updateNamedParams = new HashMap<String, Object>();
                            updateNamedParams.put(COL.LOCKED_BY, userName);
                            updateNamedParams.put(COL.UPDATED_BY, userName);
                            updateNamedParams.put(COL.LOCKED, true);

                            Query updateQuery = null;
                            try {
                                updateQuery = new Query(bucketName, true)
                                        .update()
                                        .set(updateNamedParams)
                                        .where(condition, updateNamedParams);
                            } catch (UnknownColumnException e) {
                                e.printStackTrace();
                            }

                            jdbcTemplate.update(updateQuery.toString(logger), updateNamedParams);
                            return listOfIds;
                        } else
                            return null;
                    }
                });

                done = true;
            } catch (DeadlockLoserDataAccessException d) {
                tryCount -= 1;
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                    throw new ItemDoNotExistsException("Bucket", bucketName);
                else if (e.getMessage().contains("Unknown column"))
                    throw new UnknownColumnException(getColumnName(e.getMessage()));
                else
                    throw new UnexpectedException(e);
            }
        }

        return bundlesIdsList;
    }

    public List<Map<String, Object>> getBundlesByQuery(String bucketName, Query query, Map<String, Object> namedParameters) throws ItemDoNotExistsException, UnknownColumnException, UnexpectedException {
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(query.toString(logger), namedParameters);
            convertStringToMap(result, COL.PROPERTIES);
            return result;
        } catch (BadSqlGrammarException e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else if (e.getMessage().contains("Unknown column"))
                throw new UnknownColumnException(getColumnName(e.getMessage()));
            else
                throw new UnexpectedException(e);
        } catch (Exception ee) {
            throw new UnexpectedException(ee);
        }
    }

    private List<Map<String, Object>> getBundlesDefaultColumns() {
        List<Map<String, Object>> columns = new ArrayList<Map<String, Object>>();
        final String[] staticColumns = {COL.BUNDLE_ID, COL.TAG_ID, COL.TAG_NAME, COL.LOCKED, COL.LOCKED_BY, COL.PROPERTIES, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
        for (String colName : staticColumns) {
            Map<String, Object> colMap = new HashMap<String, Object>();
            colMap.put(C.FIELD, colName);
            columns.add(colMap);
        }
        return columns;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getBundles(String bucketName, Optional<Integer[]> bundleId, Optional<Integer[]> tagId, Optional<Integer> filterId, Optional<Integer> viewId, Optional<List<Map<String, Object>>> inColumns, Optional<List<Condition>> inConditions, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort) throws IncorrectValueException, ItemDoNotExistsException, UnknownColumnException, UnexpectedException, JsonParseException, JsonMappingException, IOException {

        List<Map<String, Object>> columns = null;
        Map<String, Object> paramMap = new HashMap<String, Object>();
        List<Condition> conditions = new ArrayList<Condition>();

        Query queryData = null;
        Query queryCount = null;

        boolean joinTags = false;
        if (sort.isPresent())
            joinTags = sort.get().contains(COL.TAG_NAME);

        // ---------- ID ------------
        if (bundleId.isPresent()) {
            columns = getBundlesDefaultColumns();
            joinTags = true; // currently default columns contain tag_name
            conditions.add(new Condition(COL.BUNDLE_ID, Operator.in, new ArrayList<Integer>(Arrays.asList(bundleId.get()))));

            // ---------- FILTER ------------
        } else if (filterId.isPresent()) {
            columns = getBundlesDefaultColumns();
            joinTags = true; // currently default columns contain tag_name
            conditions = getFilterConditions(filterId.get());

            // ---------- TAG ------------
        } else if (tagId.isPresent()) {
            columns = getBundlesDefaultColumns();
            joinTags = true; // currently default columns contain tag_name
            conditions.add(new Condition(COL.TAG_ID, Operator.in, new ArrayList<Integer>(Arrays.asList(tagId.get()))));

            // ---------- VIEW ------------
        } else if (viewId.isPresent()) {

            Map<String, Object> view = getView(viewId.get());

            // Prepare columns
            boolean isBundleId = false;
            columns = (List<Map<String, Object>>) view.get(COL.COLUMNS);
            for (Map<String, Object> column : columns) {
                String field = (String) column.get(C.FIELD);

                if (field.equals(COL.TAG_NAME))
                    joinTags = true;

                if (field.equals(COL.BUNDLE_ID))
                    isBundleId = true;
            }

            if (!isBundleId) {
                Map<String, Object> colBundleId = new HashMap<String, Object>();
                colBundleId.put(C.FIELD, COL.BUNDLE_ID);
                columns.add(colBundleId);
            }

            // Filter conditions
            if (view.containsKey(COL.FILTER_ID) && view.get(COL.FILTER_ID) != null) {
                conditions = getFilterConditions((int) view.get(COL.FILTER_ID));
            }

            // View conditions
            if (view.containsKey(COL.CONDITIONS) && view.get(COL.CONDITIONS) != null) {
                List<Condition> viewConditions = (List<Condition>) view.get(COL.CONDITIONS);
                for (Condition cond : viewConditions)
                    conditions.add(cond);
            }

            // ---------- CUSTOM CONDITIONS ------------
        } else {
            // set columns
            if (inColumns.isPresent()) {
                boolean isBundleId = false;
                for (Map<String, Object> column : inColumns.get()) {
                    String field = (String) column.get("field");

                    if (field.equals("tag_name"))
                        joinTags = true;

                    if (field.equals(COL.BUNDLE_ID))
                        isBundleId = true;
                }

                columns = inColumns.get();

                if (!isBundleId) {
                    Map<String, Object> colBundleId = new HashMap<String, Object>();
                    colBundleId.put(C.FIELD, COL.BUNDLE_ID);
                    columns.add(colBundleId);
                }
            } else {
                columns = getBundlesDefaultColumns();
                joinTags = true;
            }

            if (inConditions.isPresent())
                conditions = inConditions.get();
        }

        joinTags = joinTags || usedTagNameColumn(conditions);

        queryCount = new Query(bucketName, true)
                .select(COL.COUNT)
                .from()
                .joinTags(joinTags)
                .where(conditions, paramMap);

        queryData = new Query(bucketName, true)
                .selectBundles(columns)
                .from()
                .joinTags(joinTags)
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        long count = getBundlesCount(bucketName, queryCount, paramMap);
        List<Map<String, Object>> bundles = getBundlesByQuery(bucketName, queryData, paramMap);

        // Data from properties are always as strings. This converts them into proper types.
        convertPropertiesColumns(bundles);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(C.TOTAL, count);
        result.put(C.BUNDLES, bundles);
        return result;
    }

    public int createBundle(String createdBy, String bucketName, Map<String, Object> details) throws JsonProcessingException, ItemDoNotExistsException, UnexpectedException, ItemDoNotExistsException, UnknownColumnException {
        try {
            MapSqlParameterSource namedParameters = new MapSqlParameterSource();
            if (details.containsKey(COL.TAG_ID))
                namedParameters.addValue(COL.TAG_ID, details.get(COL.TAG_ID));
            else if (details.containsKey(COL.TAG_NAME)) {
                String tagName = (String) details.get(COL.TAG_NAME);
                namedParameters.addValue(COL.TAG_ID, getTagId(tagName));
            }

            namedParameters.addValue(COL.CREATED_BY, createdBy);
            boolean added = putNotEmpty(namedParameters, COL.LOCKED, details.get(COL.LOCKED));
            if (added && (boolean) details.get(COL.LOCKED) == true)
                namedParameters.addValue(COL.LOCKED_BY, createdBy);
            putNotEmpty(namedParameters, COL.PROPERTIES, details.get(COL.PROPERTIES));

            KeyHolder keyHolder = new GeneratedKeyHolder();
            Query query = new Query(bucketName, true).insertIntoValues(namedParameters);
            jdbcTemplate.update(query.toString(logger), namedParameters, keyHolder, new String[]{COL.BUNDLE_ID});
            return keyHolder.getKey().intValue();
        } catch (BadSqlGrammarException e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public int modifyBundles(String updatedBy, String bucketName, Optional<Integer[]> bundlesIds, Optional<Integer> filterId, Optional<Integer[]> tagsIds, LinkedHashMap<String, Object> details) throws IOException, ItemDoNotExistsException, UnexpectedException, ItemDoNotExistsException, UnknownColumnException {

        List<Condition> conditions = new ArrayList<Condition>();
        if (bundlesIds.isPresent()) {
            conditions.add(new Condition(COL.BUNDLE_ID, Operator.in, new ArrayList<Integer>(Arrays.asList(bundlesIds.get()))));
        } else if (filterId.isPresent()) {
            conditions = getFilterConditions(filterId.get());
        } else if (tagsIds.isPresent()) {
            conditions.add(new Condition(COL.TAG_ID, Operator.in, new ArrayList<Integer>(Arrays.asList(tagsIds.get()))));
        } else if (details.get(COL.CONDITIONS) != null) {
            List<Map<String, Object>> bodyConditions = (List<Map<String, Object>>) details.get(COL.CONDITIONS);
            for (Map<String, Object> bodyCondition : bodyConditions)
                conditions.add(new Condition(bodyCondition));
        }

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(COL.UPDATED_BY, updatedBy);

        if (details.containsKey(COL.TAG_ID))
            putNotEmpty(details, paramMap, COL.TAG_ID);
        else if (details.containsKey(COL.TAG_NAME)) {
            int tagId = getTagId((String) details.get(COL.TAG_NAME));
            paramMap.put(COL.TAG_ID, tagId);
        }

        if (details.get(COL.LOCKED) != null) {
            boolean locked = (boolean) details.get(COL.LOCKED);
            paramMap.put(COL.LOCKED, locked);
            if (locked)
                paramMap.put(COL.LOCKED_BY, updatedBy);
            else
                paramMap.put(COL.LOCKED_BY, null);
        }

        boolean properties = putNotEmpty(details, paramMap, COL.PROPERTIES);
        boolean joinTags = usedTagNameColumn(conditions);

        Query query = new Query(bucketName, true)
                .update()
                .joinTags(joinTags)
                .set(paramMap)
                .removeProperties(!properties, details)
                .setProperties(!properties, details, paramMap)
                .where(conditions, paramMap);

        try {
            return this.jdbcTemplate.update(query.toString(logger), paramMap);
        } catch (Exception e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }

    public int deleteBundles(String bucketName, Optional<Integer[]> bundlesIds, Optional<Integer> filterId, Optional<Integer[]> tagsIds) throws JsonParseException, JsonMappingException, IOException, ItemDoNotExistsException, UnexpectedException, UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        Map<String, Object> paramMap = new HashMap<String, Object>();

        if (bundlesIds.isPresent()) {
            conditions.add(new Condition(COL.BUNDLE_ID, Operator.in, new ArrayList<Integer>(Arrays.asList(bundlesIds.get()))));
        } else if (filterId.isPresent()) {
            conditions = getFilterConditions(filterId.get());
        } else if (tagsIds.isPresent()) {
            conditions.add(new Condition(COL.TAG_ID, Operator.in, new ArrayList<Integer>(Arrays.asList(tagsIds.get()))));
        }

        Query query = new Query(bucketName, false)
                .delete()
                .from()
                .where(conditions, paramMap);

        try {
            return jdbcTemplate.update(query.toString(logger), paramMap);
        } catch (Exception e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }

    public int deleteBundles(String bucketName, List<Condition> conditions) throws ItemDoNotExistsException, UnknownColumnException, UnexpectedException {
        Map<String, Object> paramMap = new HashMap<String, Object>();

        boolean joinTags = usedTagNameColumn(conditions);

        Query query = new Query(bucketName, true)
                .delete()
                .from()
                .joinTags(joinTags)
                .where(conditions, paramMap);

        try {
            return jdbcTemplate.update(query.toString(logger), paramMap);
        } catch (Exception e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }


    public List<Map<String, Object>> getBundleHistory(String bucketName, Integer bundleId) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException {
        Map<String, Object> paramMap = new HashMap<String, Object>();

        String[] columns = {COL.ID, COL.TAG_ID, COL.LOCKED, COL.PROPERTIES + " is not null as '" + COL.PROPERTIES + "'", COL.UPDATED_AT, COL.UPDATED_BY};
        Condition condition = new Condition(COL.BUNDLE_ID, Operator.equal, bundleId);

        Query query = new Query(bucketName + "_history", true)
                .select(columns)
                .from()
                .where(condition, paramMap)
                .orderBy("a." + COL.UPDATED_AT, true);
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(query.toString(logger), paramMap);

            // clean null properties
            Integer previousTagId = -1;
            for (Map<String, Object> map : result) {

                // remove null properties items, change value into boolean
                if ((Long) map.get(COL.PROPERTIES) == 0L) {
                    map.remove(COL.PROPERTIES);
                } else {
                    map.replace(COL.PROPERTIES, true); // true means -> properties has been changed
                }

                // remove null tag_id items if previous tag_id is the same
                if (map.get(COL.TAG_ID) == null && previousTagId == null) {
                    map.remove(COL.TAG_ID);
                }
                previousTagId = (Integer) map.get(COL.TAG_ID);

                // remove null locked items
                if (map.get(COL.LOCKED) == null) {
                    map.remove(COL.LOCKED);
                }
            }

            return result;

        } catch (Exception e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }

    public List<Map<String, Object>> getBundleHistoryProperties(String bucketName, Integer bundleId, Integer[] ids) throws JsonParseException, JsonMappingException, IOException, ItemDoNotExistsException, UnexpectedException, UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        Map<String, Object> namedParameters = new HashMap<String, Object>();

        String[] columns = {COL.ID, COL.PROPERTIES};

        conditions.add(new Condition(COL.BUNDLE_ID, Operator.equal, bundleId));
        conditions.add(new Condition(COL.ID, Operator.in, new ArrayList<Integer>(Arrays.asList(ids))));

        Query query = new Query(bucketName + "_history", true)
                .select(columns)
                .from()
                .where(conditions, namedParameters)
                .orderBy(COL.ID, true);
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(query.toString(logger), namedParameters);
            convertStringToMap(result, COL.PROPERTIES);
            return result;
        } catch (Exception e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }

    public int createTag(String createdBy, String tagName, Integer bucketId, String bucketName, String iconName, String description, Integer classId) throws ItemDoNotExistsException, JsonProcessingException, TagAlreadyExistsException, InvalidDataAccessApiUsageException, DataAccessException, UnknownColumnException {

        if (!isTagExist(null, tagName)) {
            if (bucketId == null && bucketName != null) {
                bucketId = getBucketId(bucketName);
            }

            MapSqlParameterSource namedParameters = new MapSqlParameterSource();
            namedParameters.addValue(COL.TAG_NAME, tagName);
            putNotEmpty(namedParameters, COL.BUCKET_ID, bucketId);
            namedParameters.addValue(COL.CREATED_BY, createdBy);
            putNotEmpty(namedParameters, COL.DESCRIPTION, description);
            putNotEmpty(namedParameters, COL.CLASS_ID, classId);

            KeyHolder keyHolder = new GeneratedKeyHolder();
            Query query = new Query(TAB.TAG, true).insertIntoValues(namedParameters);
            jdbcTemplate.update(query.toString(logger), namedParameters, keyHolder, new String[]{COL.TAG_ID});
            return keyHolder.getKey().intValue();
        } else {
            throw new TagAlreadyExistsException(tagName);
        }
    }

    public Map<String, Object> getTags(Optional<String> bucketName, Optional<Integer> tagId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        Map<String, Object> paramMap = new HashMap<String, Object>();

        if (tagId.isPresent()) {
            conditions.add(new Condition(COL.TAG_ID, Operator.equal, tagId.get()));
        } else if (bucketName.isPresent()) {
            conditions.add(new Condition(COL.BUCKET_NAME, Operator.equal, bucketName.get()));
        }

        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (urlConditions != null)
            for (Condition condition : urlConditions)
                conditions.add(condition);

        Query queryCount = new Query(TAB.TAG, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.TAG, true)
                .select(getTagColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(C.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger), paramMap, Long.TYPE));
        result.put(C.TAGS, jdbcTemplate.queryForList(queryData.toString(logger), paramMap));

        return result;
    }

    private Map<String, Object> getEvent(int eventId) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.EVENT_ID, Operator.equal, eventId));
        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.EVENT, true)
                .select(getEventsColumns())
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> eventList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        if (eventList.size() > 0)
            return eventList.get(0);
        else
            return null;
    }

    public Map<String, Object> getEvents(Optional<String> bucketName, Optional<Integer> eventId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnknownColumnException, UnexpectedException {
        List<Condition> conditions = new ArrayList<Condition>();
        Map<String, Object> paramMap = new HashMap<String, Object>();

        if (eventId.isPresent()) {
            conditions.add(new Condition(COL.EVENT_ID, Operator.equal, eventId.get()));
        } else if (bucketName.isPresent()) {
            conditions.add(new Condition(COL.BUCKET_NAME, Operator.equal, bucketName.get()));
        }

        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (urlConditions != null)
            for (Condition condition : urlConditions)
                conditions.add(condition);

        Query queryCount = new Query(TAB.EVENT, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.EVENT, true)
                .select(getEventsColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(C.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger), paramMap, Long.TYPE));
        List<Map<String, Object>> eventList = jdbcTemplate.queryForList(queryData.toString(logger), paramMap);

        // Verify active events. All active events should have its representation in database events.
        List<String> databaseEvents = getDatabaseEventsNames();
        List<Integer> removedEventsIds = new ArrayList<Integer>();

        for (Map<String, Object> eventItem : eventList) {
            if ((boolean) eventItem.get(COL.ACTIVE) == true) {
                String eventName = (String) eventItem.get(COL.EVENT_NAME);
                if (!databaseEvents.contains(eventName))
                    removedEventsIds.add((Integer) eventItem.get(COL.EVENT_ID));
            }
        }

        if (removedEventsIds.size() > 0) {
            deactivateEvents(removedEventsIds);
            eventList = jdbcTemplate.queryForList(queryData.toString(logger), paramMap);
        }

        convertStringToList(eventList, COL.TASKS);
        convertStringToMap(eventList, COL.SCHEDULE);
        result.put(C.EVENTS, eventList);

        return result;
    }

    public Map<String, Object> getEventsLog(Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        Map<String, Object> paramMap = new HashMap<String, Object>();

        if (urlConditions != null)
            for (Condition condition : urlConditions)
                conditions.add(condition);

        Query queryCount = new Query(TAB.EVENT_LOG, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.EVENT_LOG, true)
                .select(getEventsLogColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(C.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger), paramMap, Long.TYPE));
        result.put(C.EVENTS_LOG, jdbcTemplate.queryForList(queryData.toString(logger), paramMap));

        return result;
    }

    private void deactivateEvents(List<Integer> eventsIds) throws UnknownColumnException {
        Condition condition = new Condition(COL.EVENT_ID, Operator.in, eventsIds);
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(COL.UPDATED_BY, "server");
        paramMap.put(COL.ACTIVE, false);

        Query query = new Query(TAB.EVENT, true)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        this.jdbcTemplate.update(query.toString(logger), paramMap);
    }

    public void modifyTag(String updatedBy, Integer tagId, String tagName, Integer bucketId, Integer classId, String description) throws TagAlreadyExistsException, JsonProcessingException, ItemDoNotExistsException, IncorrectValueException, ExceededMaximumNumberOfCharactersException, UnknownColumnException {
        if (isTagExist(tagId)) {

            if (tagName != null && isTagExist(tagId, tagName))
                throw new TagAlreadyExistsException(tagName);

            Condition condition = new Condition(COL.TAG_ID, Operator.equal, tagId);
            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put(COL.UPDATED_BY, updatedBy);
            putNotEmptyNullableInteger(paramMap, COL.BUCKET_ID, bucketId);
            putNotEmptyNullableInteger(paramMap, COL.CLASS_ID, classId);
            putNotEmpty(paramMap, COL.TAG_NAME, tagName);
            putNotEmpty(paramMap, COL.DESCRIPTION, description);

            Query query = new Query(TAB.TAG, true)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger), paramMap);

        } else
            throw new ItemDoNotExistsException("Tag", tagId);
    }

    public int deleteTag(String userName, int tagId) throws UnknownColumnException, ItemDoNotExistsException, ItemAlreadyUsedException {
        String message = referencesTagItem(tagId);
        if (message != null)
            throw new ItemAlreadyUsedException(message);

        Condition condition = new Condition(COL.TAG_ID, Operator.equal, tagId);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Set tag as deleted
        Query query = new Query(TAB.TAG, true)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        return jdbcTemplate.update(query.toString(logger), paramMap);
    }

    private String referencesTagItem(int tagId) throws UnknownColumnException, ItemDoNotExistsException {
        final String AS_ID = " as 'id'";
        final String AS_NAME = " as 'name'";
        String result = "";

        // get tag's bucket_id and class_id
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.TAG_ID, Operator.equal, tagId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.TAG, true)
                .select(new String[]{COL.BUCKET_ID, COL.CLASS_ID})
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);

        if (resultList.size() == 0)
            throw new ItemDoNotExistsException("Tag", tagId);

        // Get list of bucket to check the tag usage
        Integer bucketId = (Integer) resultList.get(0).get(COL.BUCKET_ID);
        Integer classId = (Integer) resultList.get(0).get(COL.CLASS_ID);
        conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
        if (bucketId != null) {
            conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));
        } else if (classId != null) {
            conditions.add(new Condition(COL.CLASS_ID, Operator.equal, classId));
        }

        query = new Query(TAB.BUCKET, true)
                .select(new String[]{COL.BUCKET_ID + AS_ID, COL.BUCKET_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> bucketList = jdbcTemplate.queryForList(query.toString(logger), paramMap);

        List<Map<String, Object>> bucketNameList = new ArrayList<Map<String, Object>>();
        conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.TAG_ID, Operator.equal, tagId));
        paramMap = new HashMap<String, Object>();
        for (Map<String, Object> bucket : bucketList) {
            String bucketName = (String) bucket.get("name");
            query = new Query(bucketName, true)
                    .select(COL.COUNT)
                    .from()
                    .where(conditions, paramMap);

            int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);
            if (count > 0)
                bucketNameList.add(bucket);
        }

        if (bucketNameList.size() > 0) {
            result += getStringWithItemsNames(C.BUCKETS, bucketNameList);
        }

        if (result.length() > 0)
            return result;
        else
            return null;
    }

    public int createColumns(String columnsName, Integer bucketId, String createdBy, List<Map<String, Object>> columns, String description, Integer classId) throws ItemDoNotExistsException, JsonProcessingException, ColumnsAlreadyExistsException, UnknownColumnException {

        if (columnsName != null) {
            if (isColumnsExist(columnsName))
                throw new ColumnsAlreadyExistsException(columnsName);
        }

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue(COL.COLUMNS_NAME, columnsName);
        namedParameters.addValue(COL.CREATED_BY, createdBy);
        namedParameters.addValue(COL.BUCKET_ID, bucketId);
        namedParameters.addValue(COL.CLASS_ID, classId);
        namedParameters.addValue(COL.COLUMNS, new ObjectMapper().writeValueAsString(columns));
        putNotEmpty(namedParameters, COL.DESCRIPTION, description);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(TAB.COLUMNS, true).insertIntoValues(namedParameters);
        jdbcTemplate.update(query.toString(logger), namedParameters, keyHolder, new String[]{COL.COLUMNS_ID});
        return keyHolder.getKey().intValue();
    }

    public int createFilter(String filterName, Integer bucketId, String createdBy, List<Map<String, Object>> conditions, String description, Integer classId) throws ItemDoNotExistsException, JsonProcessingException, FilterAlreadyExistsException, UnknownColumnException {

        if (filterName != null) {
            if (isFilterExist(filterName))
                throw new FilterAlreadyExistsException(filterName);
        }

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue(COL.FILTER_NAME, filterName);
        namedParameters.addValue(COL.CREATED_BY, createdBy);
        namedParameters.addValue(COL.CONDITIONS, new ObjectMapper().writeValueAsString(conditions));
        putNotEmpty(namedParameters, COL.DESCRIPTION, description);
        putNotEmpty(namedParameters, COL.CLASS_ID, classId);
        putNotEmpty(namedParameters, COL.BUCKET_ID, bucketId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(TAB.FILTER, true).insertIntoValues(namedParameters);
        jdbcTemplate.update(query.toString(logger), namedParameters, keyHolder, new String[]{COL.FILTER_ID});
        return keyHolder.getKey().intValue();
    }

    public int createTask(String taskName, Integer bucketId, Integer classId, String createdBy, String description, Map<String, Object> configuration) throws ItemDoNotExistsException, JsonProcessingException, UnknownColumnException {
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue(COL.TASK_NAME, taskName);
        namedParameters.addValue(COL.CREATED_BY, createdBy);
        namedParameters.addValue(COL.CONFIGURATION, new ObjectMapper().writeValueAsString(configuration));
        putNotEmpty(namedParameters, COL.DESCRIPTION, description);
        putNotEmpty(namedParameters, COL.CLASS_ID, classId);
        putNotEmpty(namedParameters, COL.BUCKET_ID, bucketId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(TAB.TASK, true).insertIntoValues(namedParameters);
        jdbcTemplate.update(query.toString(logger), namedParameters, keyHolder, new String[]{COL.TASK_ID});
        return keyHolder.getKey().intValue();
    }

    public int createEvent(String eventName, Integer bucketId, Integer classId, String userName, String description, Map<String, Object> schedule, List<Map<String, Object>> tasks, Boolean active) throws JsonProcessingException, InvalidDataAccessApiUsageException, DataAccessException, UnknownColumnException, ItemAlreadyExistsException, ParseException, UnexpectedException, EmptyInputValueException {

        // check an event exist in the _event table
        if (isEventExist(null, eventName))
            throw new ItemAlreadyExistsException("Event", eventName);

        // check an event exist in the database events
        if (isDatabaseEventExist(eventName))
            throw new ItemAlreadyExistsException("Event " + eventName + " already exist in the database!");

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue(COL.EVENT_NAME, eventName);
        namedParameters.addValue(COL.CREATED_BY, userName);

        namedParameters.addValue(COL.SCHEDULE, new ObjectMapper().writeValueAsString(schedule));
        namedParameters.addValue(COL.TASKS, new ObjectMapper().writeValueAsString(tasks));
        putNotEmpty(namedParameters, COL.DESCRIPTION, description);
        putNotEmpty(namedParameters, COL.ACTIVE, active);
        putNotEmpty(namedParameters, COL.CLASS_ID, classId);
        putNotEmpty(namedParameters, COL.BUCKET_ID, bucketId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(TAB.EVENT, true).insertIntoValues(namedParameters);
        jdbcTemplate.update(query.toString(logger), namedParameters, keyHolder, new String[]{COL.EVENT_ID});

        int eventId = keyHolder.getKey().intValue();

        if (active != null && active) {
            List<Map<String, Object>> buckets = getBuckets(bucketId, classId);
            Map<String, Object> event = getEvent(eventId);
            String eName = (String) event.get(COL.EVENT_NAME);
            createDatabaseEvent(eventId, eName, buckets, schedule, tasks);
        }

        return eventId;
    }

    public int deleteColumns(String userName, int columnsId) throws ItemDoNotExistsException, ItemAlreadyUsedException, UnknownColumnException {
        if (isColumnsExist(columnsId)) {

            String message = referencesColumsItem(columnsId);
            if (message != null)
                throw new ItemAlreadyUsedException(message);

            Condition condition = new Condition(COL.COLUMNS_ID, Operator.equal, columnsId);

            Map<String, Object> namedParameters = new HashMap<String, Object>();
            namedParameters.put(COL.DELETED, true);
            namedParameters.put(COL.UPDATED_BY, userName);

            // Set columns as deleted
            Query query = new Query(TAB.COLUMNS, true)
                    .update()
                    .set(namedParameters)
                    .where(condition, namedParameters);

            return jdbcTemplate.update(query.toString(logger), namedParameters);
        } else
            throw new ItemDoNotExistsException("Columns", columnsId);
    }

    private String referencesColumsItem(int columnsId) throws UnknownColumnException {
        final String AS_ID = " as 'id'";
        final String AS_NAME = " as 'name'";
        String result = "";

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.COLUMNS_ID, Operator.equal, columnsId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.VIEW, true)
                .select(new String[]{COL.VIEW_ID + AS_ID, COL.VIEW_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.VIEWS, resultList);

        if (result.length() > 0)
            return result;
        else
            return null;
    }

    public int deleteFilter(String userName, int filterId) throws ItemDoNotExistsException, ItemAlreadyUsedException, UnknownColumnException {
        if (isFilterExist(filterId)) {

            String message = referencesFilterItem(filterId);
            if (message != null)
                throw new ItemAlreadyUsedException(message);

            Condition condition = new Condition(COL.FILTER_ID, Operator.equal, filterId);

            Map<String, Object> namedParameters = new HashMap<String, Object>();
            namedParameters.put(COL.DELETED, true);
            namedParameters.put(COL.UPDATED_BY, userName);

            // Set filter as deleted
            Query query = new Query(TAB.FILTER, true)
                    .update()
                    .set(namedParameters)
                    .where(condition, namedParameters);

            return jdbcTemplate.update(query.toString(logger), namedParameters);
        } else
            throw new ItemDoNotExistsException("Filter", filterId);
    }

    private String referencesFilterItem(int filterId) throws UnknownColumnException {
        final String AS_ID = " as 'id'";
        final String AS_NAME = " as 'name'";
        String result = "";

        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.COLUMNS_ID, Operator.equal, filterId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.VIEW, true)
                .select(new String[]{COL.VIEW_ID + AS_ID, COL.VIEW_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(query.toString(logger), paramMap);
        result += getStringWithItemsNames(C.VIEWS, resultList);

        if (result.length() > 0)
            return result;
        else
            return null;
    }

    public int deleteTask(String userName, int taskId) throws ItemDoNotExistsException, UnknownColumnException, ItemAlreadyUsedException {
        if (isTaskExist(taskId)) {

            String message = referencesTaskItem(taskId);
            if (message != null)
                throw new ItemAlreadyUsedException(message);

            Condition condition = new Condition(COL.TASK_ID, Operator.equal, taskId);

            Map<String, Object> namedParameters = new HashMap<String, Object>();
            namedParameters.put(COL.DELETED, true);
            namedParameters.put(COL.UPDATED_BY, userName);

            // Set task as deleted
            Query query = new Query(TAB.TASK, true)
                    .update()
                    .set(namedParameters)
                    .where(condition, namedParameters);

            return jdbcTemplate.update(query.toString(logger), namedParameters);
        } else
            throw new ItemDoNotExistsException("Task", taskId);
    }

    private String referencesTaskItem(int taskId) throws UnknownColumnException {
		final String AS_ID = " as 'id'";
		final String AS_NAME = " as 'name'";
		String result = "";

		List<Condition> conditions = new ArrayList<Condition>();
		conditions.add(new Condition(COL.DELETED, Operator.equal, false));
		Map<String, Object> paramMap = new HashMap<String, Object>();

		Query query = new Query(TAB.EVENT, true)
				.select(new String[]{COL.EVENT_ID + AS_ID, COL.EVENT_NAME + AS_NAME, COL.TASKS})
				.from()
				.where(conditions, paramMap);

		List<Map<String, Object>> eventList = jdbcTemplate.queryForList(query.toString(logger), paramMap);

		if (eventList.size() > 0) {
            ObjectMapper mapper = new ObjectMapper();
		    List<Map<String, Object>> refEventList = new ArrayList<Map<String, Object>>();
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
                result += getStringWithItemsNames(C.EVENTS, refEventList);
            }
        }

        conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.TASK_ID, Operator.equal, taskId));
        query = new Query(TAB.EVENT_LOG, false)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);
        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);
        if (count > 1)
            result += "\nLOGS";

        if (result.length() > 0)
            return result;
        else
            return null;
    }

    public int deleteEvent(String userName, Integer eventId) throws DataAccessException, UnknownColumnException, ItemDoNotExistsException {
        if (isItemExist(TAB.EVENT, COL.EVENT_ID, eventId)) {

            Map<String, Object> event = getEvent(eventId);
            String eName = (String) event.get(COL.EVENT_NAME);
            removeDatabaseEvent(eName);

            Condition condition = new Condition(COL.EVENT_ID, Operator.equal, eventId);

            Map<String, Object> namedParameters = new HashMap<String, Object>();
            namedParameters.put(COL.DELETED, true);
            namedParameters.put(COL.UPDATED_BY, userName);

            // Set event as deleted
            Query query = new Query(TAB.EVENT, true)
                    .update()
                    .set(namedParameters)
                    .where(condition, namedParameters);

            return jdbcTemplate.update(query.toString(logger), namedParameters);
        } else
            throw new ItemDoNotExistsException("Event", eventId);
    }

    public void clearEventsLog() {
        Query query = new Query(TAB.EVENT_LOG, false)
                .delete()
                .from();

        jdbcTemplate.getJdbcTemplate().update(query.toString(logger));
    }

    public Map<String, Object> getColumns(Optional<String> bucketName, Optional<Integer> columnsId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (columnsId.isPresent()) {
            conditions.add(new Condition(COL.COLUMNS_ID, Operator.equal, columnsId.get()));
        } else if (bucketName.isPresent()) {
            int bucketId = getBucketId(bucketName.get());
            conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));
        }

        if (urlConditions != null)
            for (Condition condition : urlConditions)
                conditions.add(condition);

        Query queryCount = new Query(TAB.COLUMNS, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.COLUMNS, true)
                .select(getColumnsColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(C.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger), paramMap, Long.TYPE));
        List<Map<String, Object>> columnsList = jdbcTemplate.queryForList(queryData.toString(logger), paramMap);
        convertStringToList(columnsList, COL.COLUMNS);
        result.put(C.COLUMNS, columnsList);

        return result;
    }

    public Map<String, Object> getFilters(Optional<String> bucketName, Optional<Integer> filterId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (filterId.isPresent()) {
            conditions.add(new Condition(COL.FILTER_ID, Operator.equal, filterId.get()));
        } else if (bucketName.isPresent()) {
            int bucketId = getBucketId(bucketName.get());
            conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));
        }

        if (urlConditions != null)
            for (Condition condition : urlConditions)
                conditions.add(condition);

        Query queryCount = new Query(TAB.FILTER, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.FILTER, true)
                .select(getFilterColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(C.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger), paramMap, Long.TYPE));
        List<Map<String, Object>> filterList = jdbcTemplate.queryForList(queryData.toString(logger), paramMap);
        convertStringToList(filterList, COL.CONDITIONS);
        result.put(C.FILTERS, filterList);

        return result;
    }

    public Map<String, Object> getTask(int taskId) throws UnknownColumnException, UnexpectedException {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
        conditions.add(new Condition(COL.TASK_ID, Operator.equal, taskId));

        Query queryData = new Query(TAB.TASK, true)
                .select(getTaskColumns())
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> taskList = jdbcTemplate.queryForList(queryData.toString(logger), paramMap);
        convertStringToMap(taskList, COL.CONFIGURATION);
        return taskList.get(0);
    }

    public Map<String, Object> getTasks(Optional<String> bucketName, Optional<Integer> taskId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnknownColumnException, UnexpectedException {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (taskId.isPresent()) {
            conditions.add(new Condition(COL.TASK_ID, Operator.equal, taskId.get()));
        } else if (bucketName.isPresent()) {
            int bucketId = getBucketId(bucketName.get());
            conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));
        }

        if (urlConditions != null)
            for (Condition condition : urlConditions)
                conditions.add(condition);

        Query queryCount = new Query(TAB.TASK, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.TASK, true)
                .select(getTaskColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(C.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger), paramMap, Long.TYPE));
        List<Map<String, Object>> taskList = jdbcTemplate.queryForList(queryData.toString(logger), paramMap);
        convertStringToMap(taskList, COL.CONFIGURATION);
        result.put(C.TASKS, taskList);

        return result;
    }

    public void modifyColumns(String updatedBy, Integer columnsId, String columnsName, Integer bucketId, Integer classId, String description, List<Map<String, Object>> columns) throws ItemDoNotExistsException, ExceededMaximumNumberOfCharactersException, UnexpectedException, ColumnsAlreadyExistsException, UnknownColumnException {

        if (isColumnsExist(columnsId)) {
            if (columnsName != null) {
                if (isColumnsExist(columnsName))
                    throw new ColumnsAlreadyExistsException(columnsName);
            }

            Condition condition = new Condition(COL.COLUMNS_ID, Operator.equal, columnsId);
            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put(COL.UPDATED_BY, updatedBy);
            try {
                putNotEmptyNullableInteger(paramMap, COL.BUCKET_ID, bucketId);
                putNotEmptyNullableInteger(paramMap, COL.CLASS_ID, classId);
                putNotEmpty(paramMap, COL.COLUMNS_NAME, columnsName);
                putNotEmpty(paramMap, COL.DESCRIPTION, description);
                putNotEmpty(paramMap, COL.COLUMNS, columns);
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }


            Query query = new Query(TAB.COLUMNS, true)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger), paramMap);
        } else {
            throw new ItemDoNotExistsException("Columns", columnsId);
        }
    }

    public void modifyFilter(String updatedBy, Integer filterId, String filterName, Integer bucketId, Integer classId, String description, List<Map<String, Object>> conditions) throws ItemDoNotExistsException, ExceededMaximumNumberOfCharactersException, UnexpectedException, FilterAlreadyExistsException, UnknownColumnException {

        if (isFilterExist(filterId)) {
            if (filterName != null) {
                if (isFilterExist(filterName))
                    throw new FilterAlreadyExistsException(filterName);
            }

            Condition condition = new Condition(COL.FILTER_ID, Operator.equal, filterId);
            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put(COL.UPDATED_BY, updatedBy);
            try {
                putNotEmptyNullableInteger(paramMap, COL.BUCKET_ID, bucketId);
                putNotEmptyNullableInteger(paramMap, COL.CLASS_ID, classId);
                putNotEmpty(paramMap, COL.FILTER_NAME, filterName);
                putNotEmpty(paramMap, COL.DESCRIPTION, description);
                putNotEmpty(paramMap, COL.CONDITIONS, conditions);
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
            Query query = new Query(TAB.FILTER, true)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger), paramMap);
        } else {
            throw new ItemDoNotExistsException("Filter", filterId);
        }
    }

    public void modifyTask(String updatedBy, Integer taskId, String taskName, Integer bucketId, Integer classId, String description, LinkedHashMap<String, Object> configuration) throws ItemDoNotExistsException, ExceededMaximumNumberOfCharactersException, UnexpectedException, UnknownColumnException {
        if (isTaskExist(taskId)) {
            Condition condition = new Condition(COL.TASK_ID, Operator.equal, taskId);
            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put(COL.UPDATED_BY, updatedBy);
            try {
                putNotEmptyNullableInteger(paramMap, COL.BUCKET_ID, bucketId);
                putNotEmptyNullableInteger(paramMap, COL.CLASS_ID, classId);
                putNotEmpty(paramMap, COL.TASK_NAME, taskName);
                putNotEmpty(paramMap, COL.DESCRIPTION, description);
                putNotEmpty(paramMap, COL.CONFIGURATION, configuration);

            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
            Query query = new Query(TAB.TASK, true)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger), paramMap);
        } else {
            throw new ItemDoNotExistsException("Task", taskId);
        }
    }

    public void modifyEvent(String userName, Integer eventId, String eventName, Integer bucketId, Integer classId, String description, Map<String, Object> schedule, List<Map<String, Object>> tasks, Boolean active) throws UnexpectedException, UnknownColumnException, ItemDoNotExistsException, ItemAlreadyExistsException, ParseException, EmptyInputValueException {
        Map<String, Object> event = getEvent(eventId);
        if (event != null) {

            // check an event exist in the _event table
            if (eventName != null && isEventExist(eventId, eventName))
                throw new ItemAlreadyExistsException("Event", eventName);

            // check an event exist in the database events
            if (eventName != null && isDatabaseEventExist(eventName))
                throw new ItemAlreadyExistsException("Event " + eventName + " already exist in the database!");

            List<Map<String, Object>> buckets = null;
            if (active == true) {
                buckets = getBuckets(bucketId, classId);
                if (buckets.size() == 0)
                    throw new ItemDoNotExistsException("No bucket found for this configuration!");
            }

            // Remove old event
            String eName = (String) event.get(COL.EVENT_NAME);
            removeDatabaseEvent(eName);

            Condition condition = new Condition(COL.EVENT_ID, Operator.equal, eventId);
            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put(COL.UPDATED_BY, userName);
            try {
                putNotEmptyNullableInteger(paramMap, COL.BUCKET_ID, bucketId);
                putNotEmptyNullableInteger(paramMap, COL.CLASS_ID, classId);
                putNotEmpty(paramMap, COL.EVENT_NAME, eventName);
                putNotEmpty(paramMap, COL.DESCRIPTION, description);
                putNotEmpty(paramMap, COL.SCHEDULE, schedule);
                putNotEmpty(paramMap, COL.TASKS, tasks);
                putNotEmpty(paramMap, COL.ACTIVE, active);

            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
            Query query = new Query(TAB.EVENT, true)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger), paramMap);

            if (active == true) {
                if (eventName != null)
                    createDatabaseEvent(eventId, eventName, buckets, schedule, tasks);
                else
                    createDatabaseEvent(eventId, eName, buckets, schedule, tasks);
            }

        } else {
            throw new ItemDoNotExistsException("Event", eventId);
        }
    }

    public int createView(String userName, String viewName, String description, Integer bucketId, Integer classId, Integer columnsId, Integer filterId) throws UnexpectedException, ViewAlreadyExistsException, UnknownColumnException, JsonProcessingException {

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue(COL.VIEW_NAME, viewName);
        namedParameters.addValue(COL.CREATED_BY, userName);
        putNotEmpty(namedParameters, COL.BUCKET_ID, bucketId);
        putNotEmpty(namedParameters, COL.CLASS_ID, classId);
        putNotEmpty(namedParameters, COL.COLUMNS_ID, columnsId);
        putNotEmpty(namedParameters, COL.FILTER_ID, filterId);
        putNotEmpty(namedParameters, COL.DESCRIPTION, description);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(TAB.VIEW, true).insertIntoValues(namedParameters);

        try {
            jdbcTemplate.update(query.toString(logger), namedParameters, keyHolder, new String[]{COL.VIEW_ID});
            return keyHolder.getKey().intValue();
        } catch (BadSqlGrammarException e) {
            throw new UnexpectedException(e);
        }
    }

    public int deleteView(String userName, int viewId) throws UnknownColumnException {
        Condition condition = new Condition(COL.VIEW_ID, Operator.equal, viewId);

        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Set view as deleted
        Query query = new Query(TAB.VIEW, true)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        return jdbcTemplate.update(query.toString(logger), paramMap);
    }

    public void modifyView(String updatedBy, Integer viewId, String viewName, Integer bucketId, Integer classId, String description, Integer columnsId, Integer filterId) throws HarmlessException, JsonProcessingException, ItemDoNotExistsException, ItemDoNotExistsException, UnknownColumnException, ItemDoNotExistsException {
        if (isViewExist(viewId)) {

            if (filterId != null && filterId > 0 && !isFilterExist(filterId))
                throw new ItemDoNotExistsException("Filter", filterId);

            if (columnsId != null && columnsId > 0 && !isColumnsExist(columnsId))
                throw new ItemDoNotExistsException("Columns", columnsId);

            Condition condition = new Condition(COL.VIEW_ID, Operator.equal, viewId);
            Map<String, Object> paramMap = new HashMap<String, Object>();
            paramMap.put(COL.UPDATED_BY, updatedBy);
            putNotEmptyNullableInteger(paramMap, COL.BUCKET_ID, bucketId);
            putNotEmptyNullableInteger(paramMap, COL.CLASS_ID, classId);
            putNotEmptyNullableInteger(paramMap, COL.COLUMNS_ID, columnsId);
            putNotEmptyNullableInteger(paramMap, COL.FILTER_ID, filterId);
            putNotEmpty(paramMap, COL.VIEW_NAME, viewName);
            putNotEmpty(paramMap, COL.DESCRIPTION, description);

            Query query = new Query(TAB.VIEW, true)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger), paramMap);
        } else {
            throw new ItemDoNotExistsException("View", viewId);
        }
    }

    public Map<String, Object> getViews(Optional<String> bucketName, Optional<Integer> viewId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws JsonParseException, JsonMappingException, IOException, ItemDoNotExistsException, UnexpectedException, UnknownColumnException {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (viewId.isPresent()) {
            conditions.add(new Condition(COL.VIEW_ID, Operator.equal, viewId.get()));
        } else if (bucketName.isPresent()) {
            int bucketId = getBucketId(bucketName.get());
            conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));
        }

        if (urlConditions != null)
            for (Condition condition : urlConditions)
                conditions.add(condition);

        Query queryCount = new Query(TAB.VIEW, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.VIEW, true)
                .select(getViewColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put(C.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger), paramMap, Long.TYPE));
        List<Map<String, Object>> viewsList = jdbcTemplate.queryForList(queryData.toString(logger), paramMap);
        result.put(C.VIEWS, viewsList);

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Condition> getFilterConditions(int filterId) throws UnexpectedException, UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        Map<String, Object> filter = getFilter(filterId);
        List<Map<String, Object>> filterConditions = (List<Map<String, Object>>) filter.get(COL.CONDITIONS);
        for (Map<String, Object> filterCond : filterConditions)
            conditions.add(new Condition(filterCond));
        return conditions;
    }

    private Map<String, Object> getView(int viewId) throws UnexpectedException, UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.VIEW_ID, Operator.equal, viewId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.VIEW, true)
                .select(getViewColumns())
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> views = this.jdbcTemplate.queryForList(query.toString(logger), paramMap);
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

    private Map<String, Object> getFilter(int filterId) throws UnexpectedException, UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.FILTER_ID, Operator.equal, filterId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.FILTER, true)
                .select(getFilterColumns())
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> filters = jdbcTemplate.queryForList(query.toString(logger), paramMap);

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

    private String getFormattedDate(String dateInString) throws ParseException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        LocalDateTime dt = LocalDateTime.parse(dateInString, fmt);
        Instant iDate = dt.toInstant(ZoneOffset.UTC);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.systemDefault());
        return dateTimeFormatter.format(iDate);
    }

    private String getUnit(int key) {
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> getTaskActionsProperties(Map<String, Object> actions) {
        Map<String, Object> resultMap = new HashMap<String, Object>();

        List<Map<String, Object>> actionProperties = (List<Map<String, Object>>) actions.get(C.PROPERTIES);
        if (actionProperties != null && actionProperties.size() > 0) {
            List<String> propertiesToRemoveArray = new ArrayList<String>();
            Map<String, Object> propertiesToModifyMap = new HashMap<String, Object>();

            for (Map<String, Object> item : actionProperties) {
                String action = (String) item.get(C.ACTION);
                String path = (String) item.get(C.PATH);
                if (action.equals(C.REMOVE))
                    propertiesToRemoveArray.add(path);
                else if (action.equals(C.SET)) {
                    String type = (String) item.get(C.TYPE);
                    String value = (String) item.get(C.VALUE);
                    if (type.equals("numeric"))
                        propertiesToModifyMap.put(path, Float.parseFloat(value));
                    else if (type.equals("boolean"))
                        propertiesToModifyMap.put(path, value.toUpperCase().equals("TRUE"));
                    else if (type.equals("null"))
                        propertiesToModifyMap.put(path, null);
                    else
                        propertiesToModifyMap.put(path, value);
                }
            }

            if (propertiesToModifyMap.size() > 0)
                resultMap.put("update_properties", propertiesToModifyMap);

            if (propertiesToRemoveArray.size() > 0)
                resultMap.put("remove_properties", propertiesToRemoveArray);
        }

        return resultMap;
    }

    private String getTaskModifyQuery(String eventName, String bucketName, List<Condition> conditions, Map<String, Object> actions) throws UnknownColumnException {
        Map<String, Object> paramMap = new HashMap<String, Object>();
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
                .removeProperties(true, propertiesMap)
                .setPropertiesWithValues(propertiesMap)
                .where(conditions);

        return query.toString(logger);
    }

    private String getTaskRemoveQuery(String bucketName, List<Condition> conditions) throws UnknownColumnException {
        boolean joinTags = usedTagNameColumn(conditions);

        Query query = new Query(bucketName, true)
                .delete()
                .from()
                .joinTags(joinTags)
                .where(conditions);

        return query.toString(logger);
    }

    @SuppressWarnings("unchecked")
    private void createDatabaseEvent(int eventId, String eventName, List<Map<String, Object>> buckets, Map<String, Object> schedule, List<Map<String, Object>> tasks) throws ParseException, UnknownColumnException, UnexpectedException, EmptyInputValueException {
        String eventQuery = "";

        eventQuery += "CREATE EVENT `" + eventName + "`\n";
        eventQuery += "\tON SCHEDULE\n";

        // Schedule
        boolean periodically = (boolean) schedule.get(C.PERIODICALLY);
        if (periodically) {
            String starts = getFormattedDate((String) schedule.get(C.STARTS));
            Map<String, Object> interval = (Map<String, Object>) schedule.get(C.INTERVAL);
            int amount = (int) interval.get(C.AMOUNT);
            String unit = getUnit((int) interval.get(C.UNIT));
            eventQuery += "\tEVERY " + amount + " " + unit + "\n";
            eventQuery += "\tSTARTS '" + starts + "'\n";

            boolean enabled_end = (boolean) schedule.get(C.ENABLE_ENDS);
            if (enabled_end) {
                String ends = getFormattedDate((String) schedule.get(C.ENDS));
                eventQuery += "\tENDS '" + ends + "'\n";
            }
        } else {
            String at = getFormattedDate((String) schedule.get(C.STARTS));
            eventQuery += "\tAT '" + at + "'\n";
        }

        eventQuery += "DO\n";
        eventQuery += "\tBEGIN\n";

        // Tasks
        for (Map<String, Object> tableTask : tasks) {
            int taskId = Integer.parseInt((String) tableTask.get("task_id"));
            Map<String, Object> task = getTask(taskId);
            String taskName = (String) task.get(COL.TASK_NAME);
            Map<String, Object> configuration = (Map<String, Object>) task.get(C.CONFIGURATION);
            List<Condition> conditions = FieldValidator.validateListOfConditions(configuration, false);
            Map<String, Object> actions = (Map<String, Object>) configuration.get(C.ACTIONS);
            String actionType = (String) actions.get(C.TYPE);
            if (actionType.equals(C.MODIFY)) {
                for (Map<String, Object> bucket : buckets) {
                    int bucketId = (int) bucket.get(COL.BUCKET_ID);
                    String bucketName = (String) bucket.get(COL.BUCKET_NAME);
                    String modifyQuery = getTaskModifyQuery(eventName, bucketName, conditions, actions);
                    eventQuery += "\t\t/* Task: '" + taskName + "' for bucket '" + bucketName + "' */\n";
                    eventQuery += "\t\t" + modifyQuery + ";\n";
                    eventQuery += "\t\tSELECT row_count() INTO @affected_rows;\n";
                    eventQuery += "\t\tINSERT INTO `_event_log` (`event_id`, `task_id`, `bucket_id`, `affected`) VALUES (" + eventId + ", " + taskId + ", " + bucketId + ", @affected_rows);\n\n";
                }
            } else if (actionType.equals(C.REMOVE)) {
                for (Map<String, Object> bucket : buckets) {
                    int bucketId = (int) bucket.get(COL.BUCKET_ID);
                    String bucketName = (String) bucket.get(COL.BUCKET_NAME);
                    String removeQuery = getTaskRemoveQuery(bucketName, conditions);
                    eventQuery += "\t\t/* Task: '" + taskName + "' for bucket '" + bucketName + "' */\n";
                    eventQuery += "\t\t" + removeQuery + ";\n";
                    eventQuery += "\t\tSELECT row_count() INTO @affected_rows;\n";
                    eventQuery += "\t\tINSERT INTO `_event_log` (`event_id`, `task_id`, `bucket_id`, `affected`) VALUES (" + eventId + ", " + taskId + ", " + bucketId + ", @affected_rows);\n\n";
                }
            } else
                throw new UnexpectedException("Undefined action type. Expected one of the [modify, remove]. Given " + actionType);
        }

        eventQuery += "\tEND\n";

        logger.debug(eventQuery);
        jdbcTemplate.getJdbcTemplate().execute(eventQuery);

//		enableEventScheduler();
    }

    private void removeDatabaseEvent(String eventName) throws DataAccessException, UnknownColumnException {
        String query = "DROP EVENT IF EXISTS `" + eventName + "`";
        jdbcTemplate.getJdbcTemplate().execute(query);
        logger.debug(query);
//		disableEventScheduler();
    }

//	private void enableEventScheduler() throws DataAccessException, UnknownColumnException {
//		if (isActiveEventExist()) {
//			String query = "SET GLOBAL event_scheduler = ON";
//			jdbcTemplate.getJdbcTemplate().execute(query);
//			logger.debug(query);
//		}
//	}
//	
//	private void disableEventScheduler() throws DataAccessException, UnknownColumnException {
//		if (!isActiveEventExist()) {
//			String query = "SET GLOBAL event_scheduler = OFF";
//			jdbcTemplate.getJdbcTemplate().execute(query);
//			logger.debug(query);
//		}
//	}

    private boolean isBucketExist(String bucketName) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.BUCKET_NAME, Operator.equal, bucketName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.BUCKET, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);
        return count > 0;
    }

    private boolean isHistoryEnabled(String bucketName) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.BUCKET_NAME, Operator.equal, bucketName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.BUCKET, true)
                .select(COL.HISTORY)
                .from()
                .where(conditions, paramMap);

        return jdbcTemplate.queryForObject(query.toString(logger), paramMap, Boolean.class);
    }

    private int getBucketId(String bucketName) throws ItemDoNotExistsException, UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.BUCKET_NAME, Operator.equal, bucketName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.BUCKET, true)
                .select(COL.BUCKET_ID)
                .from()
                .where(conditions, paramMap);

        try {
            return jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            throw new ItemDoNotExistsException("Bucket", bucketName);
        }
    }

    private int getTagId(String tagName) throws ItemDoNotExistsException, UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.TAG_NAME, Operator.equal, tagName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.TAG, true)
                .select(COL.TAG_ID)
                .from()
                .where(conditions, paramMap);

        try {
            return jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            throw new ItemDoNotExistsException("Tag", tagName);
        }
    }

//	private boolean isActiveEventExist() throws UnknownColumnException {
//		List<Condition> conditions = new ArrayList<Condition>();
//		conditions.add(new Condition(COL.ACTIVE, Operator.equal, true));
//		conditions.add(new Condition(COL.DELETED, Operator.equal, false));
//		
//		Map<String, Object> paramMap = new HashMap<String, Object>();
//		
//		Query query = new Query(TAB.EVENT, true)
//				.select(COL.COUNT)
//				.from()
//				.where(conditions, paramMap);	
//		
//		int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);
//		
//		return count > 0;
//	}

    private boolean isTagExist(Integer tagId) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.TAG_ID, Operator.equal, tagId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.TAG, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isTagExist(Integer tagId, String tagName) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.TAG_NAME, Operator.equal, tagName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (tagId != null)
            conditions.add(new Condition(COL.TAG_ID, Operator.notEqual, tagId));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.TAG, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isEventExist(Integer eventId, String eventName) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.EVENT_NAME, Operator.equal, eventName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (eventId != null)
            conditions.add(new Condition(COL.EVENT_ID, Operator.notEqual, eventId));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.EVENT, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isDatabaseEventExist(String eventName) {
        String query = "SHOW EVENTS WHERE name = '" + eventName + "'";

        logger.debug(query);
        List<Map<String, Object>> result = jdbcTemplate.getJdbcTemplate().queryForList(query);

        return result.size() > 0;
    }

    private List<Map<String, Object>> getDatabaseEvents() {
        String query = "SHOW EVENTS";

        logger.debug(query);
        List<Map<String, Object>> result = jdbcTemplate.getJdbcTemplate().queryForList(query);

        return result;
    }

    private List<String> getDatabaseEventsNames() {
        List<Map<String, Object>> events = getDatabaseEvents();
        List<String> names = new ArrayList<>();
        for (Map<String, Object> event : events)
            names.add((String) event.get("name"));

        return names;
    }

    private boolean isGroupExist(Integer groupId, String groupName) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.GROUP_NAME, Operator.equal, groupName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (groupId != null)
            conditions.add(new Condition(COL.GROUP_ID, Operator.notEqual, groupId));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.GROUP, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isClassExist(Integer classId, String className) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.CLASS_NAME, Operator.equal, className));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (classId != null)
            conditions.add(new Condition(COL.CLASS_ID, Operator.notEqual, classId));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.CLASS, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isColumnsExist(Integer columnsId) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.COLUMNS_ID, Operator.equal, columnsId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.COLUMNS, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isFilterExist(Integer filterId) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.FILTER_ID, Operator.equal, filterId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.FILTER, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isItemExist(String table, String column, Integer id) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(column, Operator.equal, id));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(table, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isTaskExist(Integer taskId) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.TASK_ID, Operator.equal, taskId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.TASK, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isGroupExist(Integer groupId) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.GROUP_ID, Operator.equal, groupId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.GROUP, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isClassExist(Integer classId) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.CLASS_ID, Operator.equal, classId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.CLASS, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isColumnsExist(String columnsName) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.COLUMNS_NAME, Operator.equal, columnsName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.COLUMNS, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isFilterExist(String filterName) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.FILTER_NAME, Operator.equal, filterName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.FILTER, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isGroupExist(String groupName) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.GROUP_NAME, Operator.equal, groupName));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.GROUP, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isClassExist(String className) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.CLASS_NAME, Operator.equal, className));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.CLASS, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    private boolean isViewExist(Integer viewId) throws UnknownColumnException {
        List<Condition> conditions = new ArrayList<Condition>();
        conditions.add(new Condition(COL.VIEW_ID, Operator.equal, viewId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<String, Object>();

        Query query = new Query(TAB.VIEW, true)
                .select(COL.COUNT)
                .from()
                .where(conditions, paramMap);

        int count = jdbcTemplate.queryForObject(query.toString(logger), paramMap, Integer.class);

        return count > 0;
    }

    // when properties items are using in selected columns (e.g. select properties->'$.item' from bucket) then MySQL keep all data as Strings. This method converts items into proper types.
    private void convertPropertiesColumns(List<Map<String, Object>> source) {
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

    private void convertStringToMap(List<Map<String, Object>> source, String targetItemName) throws UnexpectedException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (int i = 0; i < source.size(); i++) {
                Map<String, Object> itemMap = source.get(i);
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

    private void convertStringToList(List<Map<String, Object>> source, String targetItemName) throws UnexpectedException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (int i = 0; i < source.size(); i++) {
                Map<String, Object> itemMap = source.get(i);
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

    private void convertStringToArray(List<Map<String, Object>> source, String targetItemName) throws UnexpectedException {
        try {
            ObjectMapper mapper = new ObjectMapper();
            for (int i = 0; i < source.size(); i++) {
                Map<String, Object> itemMap = source.get(i);
                String targetItemValueStr = (String) itemMap.get(targetItemName);
                if (targetItemValueStr != null) {
                    List<Integer> targetItemList = mapper.readValue(targetItemValueStr, new TypeReference<List<Integer>>() {
                    });
                    itemMap.put(targetItemName, targetItemList);
                }
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    private List<Map<String, Object>> convertBucketInfo(List<Map<String, Object>> source) {
        Map<String, Map<String, Object>> tempMap = new LinkedHashMap<String, Map<String, Object>>();
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        String key = null;
        Integer tagId = null;
        String tagName = null;

        for (Map<String, Object> map : source) {
            if (map.get(COL.TAG_ID) != null)
                tagId = (Integer) map.get(COL.TAG_ID);
            else
                tagId = null;

            if (map.get(COL.TAG_NAME) != null) {
                tagName = (String) map.get(COL.TAG_NAME);
                key = tagName;
            } else {
                tagName = null;
                key = "<?*--*?>";
            }

            if (tempMap.containsKey(key)) {
                if ((Boolean) map.get(COL.LOCKED))
                    tempMap.get(key).put("locked", map.get(COL.COUNT));
                else
                    tempMap.get(key).put("unlocked", map.get(COL.COUNT));
            } else {
                Map<String, Object> tempMapItem = new HashMap<String, Object>();
                tempMapItem.put(COL.TAG_ID, tagId);
                tempMapItem.put(COL.TAG_NAME, tagName);
                if ((Boolean) map.get(COL.LOCKED)) {
                    tempMapItem.put("locked", map.get(COL.COUNT));
                    tempMapItem.put("unlocked", 0);
                } else {
                    tempMapItem.put("locked", 0);
                    tempMapItem.put("unlocked", map.get(COL.COUNT));
                }
                tempMap.put(key, tempMapItem);
            }

        }

        for (Entry<String, Map<String, Object>> entry : tempMap.entrySet()) {
            result.add(entry.getValue());
        }

        return result;
    }

    private boolean putNotEmpty(MapSqlParameterSource map, String name, Object value) throws JsonProcessingException {
        if (value != null) {
            if (value instanceof LinkedHashMap || value instanceof ArrayList)
                map.addValue(name, new ObjectMapper().writeValueAsString(value));
            else
                map.addValue(name, value);
            return true;
        }
        return false;
    }

    private boolean putNotEmpty(Map<String, Object> source, Map<String, Object> paramMap, String key) throws JsonProcessingException {
        if (source.containsKey(key)) {
            Object value = source.get(key);
            if (value instanceof LinkedHashMap || value instanceof ArrayList)
                paramMap.put(key, new ObjectMapper().writeValueAsString(value));
            else
                paramMap.put(key, value);
            return true;
        }
        return false;
    }

    private boolean putNotEmpty(Map<String, Object> paramMap, String name, Object value) throws JsonProcessingException {
        if (value != null) {
            if (value instanceof Map || value instanceof List)
                paramMap.put(name, new ObjectMapper().writeValueAsString(value));
            else
                paramMap.put(name, value);
            return true;
        }
        return false;
    }

    // -1 means null
    private boolean putNotEmptyNullableInteger(Map<String, Object> paramMap, String name, Object value) {
        if (value != null) {
            Integer vInt = (Integer) value;
            if (vInt != -1)
                paramMap.put(name, vInt);
            else
                paramMap.put(name, null);
            return true;
        }
        return false;
    }

    private boolean usedTagNameColumn(List<Condition> urlConditions) {
        if (urlConditions != null)
            for (Condition condition : urlConditions)
                if (condition.getLeftValue().equals(COL.TAG_NAME))
                    return true;
        return false;
    }

    private String getColumnName(String message) {
        Pattern pattern = Pattern.compile("'b\\.(.+?)'");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        } else
            return null;
    }

    private String[] getViewColumns() {
        String[] columns = {COL.VIEW_ID, COL.VIEW_NAME, COL.DESCRIPTION, COL.CLASS_ID, COL.BUCKET_ID, COL.FILTER_ID, COL.COLUMNS_ID, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
        return columns;
    }

    private String[] getColumnsColumns() {
        String[] columns = {COL.COLUMNS_ID, COL.COLUMNS_NAME, COL.CLASS_ID, COL.BUCKET_ID, COL.COLUMNS, COL.DESCRIPTION, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
        return columns;
    }

    private String[] getFilterColumns() {
        String[] columns = {COL.FILTER_ID, COL.FILTER_NAME, COL.CLASS_ID, COL.BUCKET_ID, COL.CONDITIONS, COL.DESCRIPTION, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
        return columns;
    }

    private String[] getTagColumns() {
        String[] columns = {COL.TAG_ID, COL.TAG_NAME, COL.CLASS_ID, COL.BUCKET_ID, COL.DESCRIPTION, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
        return columns;
    }

    private String[] getBucketColumns() {
        String[] columns = {COL.BUCKET_ID, COL.BUCKET_NAME, COL.CLASS_ID, COL.INDEX, COL.DESCRIPTION, COL.ICON_NAME, COL.HISTORY, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
        return columns;
    }

    private String[] getGroupColumns() {
        String[] columns = {COL.GROUP_ID, COL.GROUP_NAME, COL.DESCRIPTION, COL.BUCKETS, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
        return columns;
    }

    private String[] getClassColumns() {
        String[] columns = {COL.CLASS_ID, COL.CLASS_NAME, COL.DESCRIPTION, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
        return columns;
    }

    private String[] getTaskColumns() {
        String[] columns = {COL.TASK_ID, COL.TASK_NAME, COL.CLASS_ID, COL.BUCKET_ID, COL.DESCRIPTION, COL.CONFIGURATION, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
        return columns;
    }

    private String[] getEventsColumns() {
        String[] events = {COL.EVENT_ID, COL.EVENT_NAME, COL.CLASS_ID, COL.BUCKET_ID, COL.DESCRIPTION, COL.SCHEDULE, COL.TASKS, COL.ACTIVE, COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
        return events;
    }

    private String[] getEventsLogColumns() {
        String[] events = {COL.EVENT_LOG_ID, COL.EVENT_ID, COL.TASK_ID, COL.BUCKET_ID, COL.AFFECTED, COL.CREATED_AT};
        return events;
    }

}
