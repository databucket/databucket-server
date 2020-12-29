package pl.databucket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
public class FilterService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ServiceUtils serviceUtils;
    Logger logger = LoggerFactory.getLogger(FilterService.class);

    public FilterService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.serviceUtils = new ServiceUtils(jdbcTemplate, logger);
    }

    public int createFilter(String filterName, Integer bucketId, String createdBy, List<Map<String, Object>> conditions, String description, Integer classId) throws JsonProcessingException, FilterAlreadyExistsException, UnknownColumnException, SQLException, ConditionNotAllowedException {

        if (filterName != null) {
            if (serviceUtils.isFilterExist(filterName))
                throw new FilterAlreadyExistsException(filterName);
        }

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue(COL.FILTER_NAME, filterName);
        namedParameters.addValue(COL.CREATED_BY, createdBy);
        namedParameters.addValue(COL.CONDITIONS, serviceUtils.javaObjectToPGObject(conditions));
        serviceUtils.putNotEmpty(namedParameters, COL.DESCRIPTION, description);
        serviceUtils.putNotEmpty(namedParameters, COL.CLASS_ID, classId);
        serviceUtils.putNotEmpty(namedParameters, COL.BUCKET_ID, bucketId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(TAB.FILTER, true).insertIntoValues(namedParameters);
        jdbcTemplate.update(query.toString(logger, null), namedParameters, keyHolder, new String[]{COL.FILTER_ID});
        return keyHolder.getKey().intValue();
    }

    public Map<ResultField, Object> getFilters(Optional<String> bucketName, Optional<Integer> filterId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException, ConditionNotAllowedException {
        Map<String, Object> paramMap = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (filterId.isPresent()) {
            conditions.add(new Condition(COL.FILTER_ID, Operator.equal, filterId.get()));
        } else if (bucketName.isPresent()) {
            int bucketId = serviceUtils.getBucketId(bucketName.get());
            conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));
        }

        if (urlConditions != null)
            conditions.addAll(urlConditions);

        Query queryCount = new Query(TAB.FILTER, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.FILTER, true)
                .select(serviceUtils.getFilterColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<ResultField, Object> result = new HashMap<>();
        result.put(ResultField.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger, paramMap), paramMap, Long.TYPE));
        List<Map<String, Object>> filterList = jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap);
        serviceUtils.convertStringToList(filterList, COL.CONDITIONS);
        result.put(ResultField.DATA, filterList);

        return result;
    }

    public void modifyFilter(String updatedBy, Integer filterId, String filterName, Integer bucketId, Integer classId, String description, List<Map<String, Object>> conditions) throws ItemDoNotExistsException, UnexpectedException, FilterAlreadyExistsException, UnknownColumnException, ConditionNotAllowedException {
        if (serviceUtils.isFilterExist(filterId)) {
            if (filterName != null) {
                if (serviceUtils.isFilterExist(filterName))
                    throw new FilterAlreadyExistsException(filterName);
            }

            Condition condition = new Condition(COL.FILTER_ID, Operator.equal, filterId);
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put(COL.UPDATED_BY, updatedBy);
            try {
                serviceUtils.putNotEmptyNullableInteger(paramMap, COL.BUCKET_ID, bucketId);
                serviceUtils.putNotEmptyNullableInteger(paramMap, COL.CLASS_ID, classId);
                serviceUtils.putNotEmpty(paramMap, COL.FILTER_NAME, filterName);
                serviceUtils.putNotEmpty(paramMap, COL.DESCRIPTION, description);
                serviceUtils.putNotEmpty(paramMap, COL.CONDITIONS, conditions);
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
            Query query = new Query(TAB.FILTER, false)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
        } else {
            throw new ItemDoNotExistsException("Filter", filterId);
        }
    }

    public void deleteFilter(String userName, int filterId) throws ItemDoNotExistsException, ItemAlreadyUsedException, UnknownColumnException, ConditionNotAllowedException {
        if (serviceUtils.isFilterExist(filterId)) {

            String message = referencesFilterItem(filterId);
            if (message != null)
                throw new ItemAlreadyUsedException(message);

            Condition condition = new Condition(COL.FILTER_ID, Operator.equal, filterId);

            Map<String, Object> namedParameters = new HashMap<>();
            namedParameters.put(COL.DELETED, true);
            namedParameters.put(COL.UPDATED_BY, userName);

            // Set filter as deleted
            Query query = new Query(TAB.FILTER, false)
                    .update()
                    .set(namedParameters)
                    .where(condition, namedParameters);

            jdbcTemplate.update(query.toString(logger, namedParameters), namedParameters);
        } else
            throw new ItemDoNotExistsException("Filter", filterId);
    }

    private String referencesFilterItem(int filterId) throws UnknownColumnException, ConditionNotAllowedException {
        final String AS_ID = " as \"id\"";
        final String AS_NAME = " as \"name\"";
        String result = "";

        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.FILTER_ID, Operator.equal, filterId));
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        Map<String, Object> paramMap = new HashMap<>();

        Query query = new Query(TAB.VIEW, true)
                .select(new String[]{COL.VIEW_ID + AS_ID, COL.VIEW_NAME + AS_NAME})
                .from()
                .where(conditions, paramMap);

        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
        result += serviceUtils.getStringWithItemsNames(C.VIEWS, resultList);

        if (result.length() > 0)
            return result;
        else
            return null;
    }


}
