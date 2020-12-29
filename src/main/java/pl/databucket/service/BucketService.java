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
public class BucketService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ServiceUtils serviceUtils;
    Logger logger = LoggerFactory.getLogger(BucketService.class);

    public BucketService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.serviceUtils = new ServiceUtils(jdbcTemplate, logger);
    }

    public int createBucket(String createdBy, String bucketName, int index, String description, String icon, boolean history, Integer classId) throws BucketAlreadyExistsException, ConditionNotAllowedException, UnknownColumnException {
        if (!serviceUtils.isBucketExist(bucketName)) {

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

            jdbcTemplate.update(query.toString(logger, null), namedParameters, keyHolder, new String[]{COL.BUCKET_ID});
            int bucketId = keyHolder.getKey().intValue();

            // Create table for data
            String sql = "CREATE TABLE public.\"" + bucketName + "\" ("
                    + "data_id bigserial NOT NULL,"
                    + "tag_id smallint NULL,"
                    + "locked boolean NOT NULL DEFAULT false,"
                    + "locked_by character varying(50) DEFAULT NULL,"
                    + "properties jsonb NOT NULL DEFAULT '{}'::jsonb,"
                    + "created_at timestamp with time zone NOT NULL DEFAULT current_timestamp,"
                    + "created_by character varying(50) NOT NULL,"
                    + "updated_at timestamp with time zone NOT NULL DEFAULT current_timestamp,"
                    + "updated_by character varying(50),"
                    + "PRIMARY KEY (data_id),"
                    + "CONSTRAINT fk_tag_id FOREIGN KEY(tag_id) REFERENCES _tag(tag_id))";

            logger.debug(sql);
            jdbcTemplate.getJdbcTemplate().execute(sql);

            // Create table for history
            sql = "CREATE TABLE \"" + bucketName + "_h\" ("
                    + "id bigserial NOT NULL,"
                    + "data_id bigint NOT NULL,"
                    + "tag_id smallint DEFAULT NULL,"
                    + "locked boolean DEFAULT NULL,"
                    + "properties jsonb DEFAULT NULL,"
                    + "updated_at timestamp with time zone NOT NULL DEFAULT current_timestamp,"
                    + "updated_by character varying(50),"
                    + "PRIMARY KEY (id),"
                    + "CONSTRAINT fk_tag_h_id FOREIGN KEY(data_id) REFERENCES \"" + bucketName + "\"(data_id))";

            logger.debug(sql);
            jdbcTemplate.getJdbcTemplate().execute(sql);

            createBeforeDeleteTrigger(bucketName);

            // Create after insert and after update triggers if history is enabled
            if (history) {
                createAfterInsertTrigger(bucketName);
                createAfterUpdateTrigger(bucketName);
            }

            return bucketId;
        } else {
            throw new BucketAlreadyExistsException(bucketName);
        }
    }

    public Map<ResultField, Object> getBuckets(Optional<String> bucketName, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException, DataAccessException, ConditionNotAllowedException {

        Map<String, Object> paramMap = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (bucketName.isPresent())
            conditions.add(new Condition(COL.BUCKET_NAME, Operator.equal, bucketName.get()));

        if (urlConditions != null)
            conditions.addAll(urlConditions);

        Query queryCount = new Query(TAB.BUCKET, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.BUCKET, true)
                .select(serviceUtils.getBucketColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<ResultField, Object> result = new HashMap<>();
        result.put(ResultField.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger, paramMap), paramMap, Long.TYPE));
        result.put(ResultField.DATA, jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap));

        return result;
    }

    public void deleteBucket(String bucketName, String userName) throws ItemDoNotExistsException, UnknownColumnException, ItemAlreadyUsedException, ConditionNotAllowedException {
        if (!serviceUtils.isBucketExist(bucketName))
            throw new ItemDoNotExistsException("Bucket", bucketName);

        int bucketId = serviceUtils.getBucketId(bucketName);

        String message = referencesBucketItem(bucketId);
        if (message != null)
            throw new ItemAlreadyUsedException(message);

        Condition condition = new Condition(COL.BUCKET_ID, Operator.equal, bucketId);

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Set bucket as deleted
        Query query = new Query(TAB.BUCKET, false)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        paramMap.put(COL.BUCKET_ID, bucketId);
        jdbcTemplate.update(query.toString(logger, paramMap), paramMap);

        paramMap = new HashMap<>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Delete tags from _tags
        query = new Query(TAB.TAG, false)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        paramMap.put(COL.BUCKET_ID, bucketId);
        jdbcTemplate.update(query.toString(logger, paramMap), paramMap);

        paramMap = new HashMap<>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Delete filters from _filters
        query = new Query(TAB.FILTER, false)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        paramMap.put(COL.BUCKET_ID, bucketId);
        jdbcTemplate.update(query.toString(logger, paramMap), paramMap);

        paramMap = new HashMap<>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Delete views from _views
        query = new Query(TAB.VIEW, false)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        paramMap.put(COL.BUCKET_ID, bucketId);
        jdbcTemplate.update(query.toString(logger, paramMap), paramMap);

        // Drop bucket history table
        query = new Query(bucketName + "_h", false)
                .dropTable();

        jdbcTemplate.getJdbcTemplate().execute(query.toString(logger, paramMap));

        // Drop bucket table
        query = new Query(bucketName, false)
                .dropTable();

        jdbcTemplate.getJdbcTemplate().execute(query.toString(logger, paramMap));
    }

    private String referencesBucketItem(int bucketId) throws UnknownColumnException, ConditionNotAllowedException {
        final String AS_ID = " as \"id\"";
        final String AS_NAME = " as \"name\"";
        String result = "";

        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.TAG, true)
                .select(new String[]{COL.TAG_ID + AS_ID, COL.TAG_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);
        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
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


    public void modifyBucket(String updatedBy, String bucketName, Map<String, Object> details) throws ItemDoNotExistsException, BucketAlreadyExistsException, JsonProcessingException, IncorrectValueException, ExceededMaximumNumberOfCharactersException, DataAccessException, UnknownColumnException, SQLException, ConditionNotAllowedException {
        Map<ResultField, Object> result = getBuckets(Optional.of(bucketName), Optional.empty(), Optional.empty(), Optional.empty(), null);
        List<Map<String, Object>> buckets = (List<Map<String, Object>>) result.get(ResultField.DATA);

        if (buckets.size() > 0) {
            Map<String, Object> bucket = buckets.get(0);

            if (details.containsKey(COL.BUCKET_NAME)) {
                String newBucketName = (String) details.get(COL.BUCKET_NAME);
                if (newBucketName != null) {
                    String currentBucketName = (String) bucket.get(COL.BUCKET_NAME);
                    if (!newBucketName.equals(currentBucketName)) {
                        if (!newBucketName.equalsIgnoreCase(bucketName)) {
                            if (!serviceUtils.isBucketExist(newBucketName)) {
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

            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put(COL.UPDATED_BY, updatedBy);
            serviceUtils.putNotEmpty(details, paramMap, COL.BUCKET_NAME);
            serviceUtils.putNotEmpty(details, paramMap, COL.DESCRIPTION);
            serviceUtils.putNotEmpty(details, paramMap, COL.INDEX);
            serviceUtils.putNotEmpty(details, paramMap, COL.ICON_NAME);
            serviceUtils.putNotEmpty(details, paramMap, COL.HISTORY);

            if (details.containsKey(COL.CLASS_ID)) {
                Integer classId = (Integer) details.get(COL.CLASS_ID);
                paramMap.put(COL.CLASS_ID, classId);
            }

            Query query = new Query(TAB.BUCKET, false)
                    .update()
                    .set(paramMap)
                    .where(conBucketId, paramMap);

            this.jdbcTemplate.update(query.toString(logger, paramMap), paramMap);

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

    private void modifyBucketName(String bucketName, String newBucketName) {
        // Change table names
        String sql = "ALTER TABLE \"" + bucketName + "\" RENAME TO \"" + newBucketName + "\"";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);

        sql = "ALTER TABLE \"" + bucketName + "_h\" RENAME TO \"" + newBucketName + "_h\"";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void createBeforeDeleteTrigger(String bucketName) {
        removeBeforeDeleteTrigger(bucketName);

        String sql = "CREATE TRIGGER trigger_before_delete\n" +
                "BEFORE DELETE\n" +
                "ON \"" + bucketName + "\"" +
                "FOR EACH ROW\n" +
                "EXECUTE PROCEDURE before_delete()";

        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void createAfterInsertTrigger(String bucketName) {
        removeAfterInsertTrigger(bucketName);

        String sql = "CREATE TRIGGER trigger_after_insert\n" +
                "AFTER INSERT\n" +
                "ON \"" + bucketName + "\"\n" +
                "FOR EACH ROW\n" +
                "EXECUTE PROCEDURE after_insert()";

        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void createAfterUpdateTrigger(String bucketName) {
        removeAfterUpdateTrigger(bucketName);

        String sql = "CREATE TRIGGER trigger_after_update\n" +
                "AFTER UPDATE\n" +
                "ON \"" + bucketName + "\"\n" +
                "FOR EACH ROW\n" +
                "EXECUTE PROCEDURE after_update()";

        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void removeBeforeDeleteTrigger(String bucketName) {
        String sql = "DROP TRIGGER IF EXISTS trigger_before_delete on \"" + bucketName + "\"";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void removeAfterInsertTrigger(String bucketName) {
        String sql = "DROP TRIGGER IF EXISTS trigger_after_insert on \"" + bucketName + "\"";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

    private void removeAfterUpdateTrigger(String bucketName) {
        String sql = "DROP TRIGGER IF EXISTS trigger_after_update on \"" + bucketName + "\"";
        logger.debug(sql);
        jdbcTemplate.getJdbcTemplate().execute(sql);
    }

}
