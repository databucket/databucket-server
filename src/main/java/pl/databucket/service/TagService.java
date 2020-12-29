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
public class TagService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ServiceUtils serviceUtils;
    Logger logger = LoggerFactory.getLogger(TagService.class);

    public TagService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.serviceUtils = new ServiceUtils(jdbcTemplate, logger);
    }

    public int createTag(String createdBy, String tagName, Integer bucketId, String bucketName, String description, Integer classId) throws ItemDoNotExistsException, JsonProcessingException, TagAlreadyExistsException, DataAccessException, UnknownColumnException, SQLException, ConditionNotAllowedException {

        if (!serviceUtils.isTagExist(null, tagName)) {
            if (bucketId == null && bucketName != null) {
                bucketId = serviceUtils.getBucketId(bucketName);
            }

            MapSqlParameterSource namedParameters = new MapSqlParameterSource();
            namedParameters.addValue(COL.TAG_NAME, tagName);
            serviceUtils.putNotEmpty(namedParameters, COL.BUCKET_ID, bucketId);
            namedParameters.addValue(COL.CREATED_BY, createdBy);
            serviceUtils.putNotEmpty(namedParameters, COL.DESCRIPTION, description);
            serviceUtils.putNotEmpty(namedParameters, COL.CLASS_ID, classId);

            KeyHolder keyHolder = new GeneratedKeyHolder();
            Query query = new Query(TAB.TAG, true).insertIntoValues(namedParameters);
            jdbcTemplate.update(query.toString(logger, null), namedParameters, keyHolder, new String[]{COL.TAG_ID});
            return keyHolder.getKey().intValue();
        } else {
            throw new TagAlreadyExistsException(tagName);
        }
    }

    public Map<ResultField, Object> getTags(Optional<String> bucketName, Optional<Integer> tagId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        Map<String, Object> paramMap = new HashMap<>();

        if (tagId.isPresent()) {
            conditions.add(new Condition(COL.TAG_ID, Operator.equal, tagId.get()));
        } else if (bucketName.isPresent()) {
            int bucketId = serviceUtils.getBucketId(bucketName.get());
            conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));
        }

        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (urlConditions != null)
            conditions.addAll(urlConditions);

        Query queryCount = new Query(TAB.TAG, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.TAG, true)
                .select(serviceUtils.getTagColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<ResultField, Object> result = new HashMap<>();
        result.put(ResultField.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger, paramMap), paramMap, Long.TYPE));
        result.put(ResultField.DATA, jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap));

        return result;
    }

    public void modifyTag(String updatedBy, Integer tagId, String tagName, Integer bucketId, Integer classId, String description) throws TagAlreadyExistsException, JsonProcessingException, ItemDoNotExistsException, UnknownColumnException, SQLException, ConditionNotAllowedException {
        if (serviceUtils.isTagExist(tagId)) {

            if (tagName != null && serviceUtils.isTagExist(tagId, tagName))
                throw new TagAlreadyExistsException(tagName);

            Condition condition = new Condition(COL.TAG_ID, Operator.equal, tagId);
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put(COL.UPDATED_BY, updatedBy);
            serviceUtils.putNotEmptyNullableInteger(paramMap, COL.BUCKET_ID, bucketId);
            serviceUtils.putNotEmptyNullableInteger(paramMap, COL.CLASS_ID, classId);
            serviceUtils.putNotEmpty(paramMap, COL.TAG_NAME, tagName);
            serviceUtils.putNotEmpty(paramMap, COL.DESCRIPTION, description);

            Query query = new Query(TAB.TAG, false)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger, paramMap), paramMap);

        } else
            throw new ItemDoNotExistsException("Tag", tagId);
    }

    public void deleteTag(String userName, int tagId) throws UnknownColumnException, ItemDoNotExistsException, ItemAlreadyUsedException, ConditionNotAllowedException {
        String message = referencesTagItem(tagId);
        if (message != null)
            throw new ItemAlreadyUsedException(message);

        Condition condition = new Condition(COL.TAG_ID, Operator.equal, tagId);

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Set tag as deleted
        Query query = new Query(TAB.TAG, false)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
    }

    private String referencesTagItem(int tagId) throws UnknownColumnException, ItemDoNotExistsException, ConditionNotAllowedException {
        final String AS_ID = " as \"id\"";
        final String AS_NAME = " as \"name\"";
        String result = "";

        // get tag's bucket_id and class_id
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.TAG_ID, Operator.equal, tagId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.TAG, true)
                .select(new String[]{COL.BUCKET_ID, COL.CLASS_ID})
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);

        if (resultList.size() == 0)
            throw new ItemDoNotExistsException("Tag", tagId);

        // Get list of bucket to check the tag usage
        Integer bucketId = (Integer) resultList.get(0).get(COL.BUCKET_ID);
        Integer classId = (Integer) resultList.get(0).get(COL.CLASS_ID);
        conditions = new ArrayList<>();
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

        List<Map<String, Object>> bucketList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);

        List<Map<String, Object>> bucketNameList = new ArrayList<>();
        conditions = new ArrayList<>();
        conditions.add(new Condition(COL.TAG_ID, Operator.equal, tagId));
        paramMap = new HashMap<>();
        for (Map<String, Object> bucket : bucketList) {
            String bucketName = (String) bucket.get("name");
            query = new Query(bucketName, true)
                    .select(COL.COUNT)
                    .from()
                    .where(conditions, paramMap);

            int count = jdbcTemplate.queryForObject(query.toString(logger, paramMap), paramMap, Integer.class);
            if (count > 0)
                bucketNameList.add(bucket);
        }

        if (bucketNameList.size() > 0) {
            result += serviceUtils.getStringWithItemsNames(C.BUCKETS, bucketNameList);
        }

        if (result.length() > 0)
            return result;
        else
            return null;
    }

}
