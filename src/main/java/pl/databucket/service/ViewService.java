package pl.databucket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import pl.databucket.old_service.ServiceUtils;
import pl.databucket.database.*;
import pl.databucket.exception.ConditionNotAllowedException;
import pl.databucket.exception.ItemDoNotExistsException;
import pl.databucket.exception.UnexpectedException;
import pl.databucket.exception.UnknownColumnException;

import java.sql.SQLException;
import java.util.*;


@Service
public class ViewService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ServiceUtils serviceUtils;
    Logger logger = LoggerFactory.getLogger(ViewService.class);

    public ViewService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.serviceUtils = new ServiceUtils(jdbcTemplate, logger);
    }

    public int createView(String userName, String viewName, String description, Integer bucketId, Integer classId, Integer columnsId, Integer filterId) throws UnexpectedException, JsonProcessingException, SQLException {

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue(COL.VIEW_NAME, viewName);
        namedParameters.addValue(COL.CREATED_BY, userName);
        serviceUtils.putNotEmpty(namedParameters, COL.BUCKET_ID, bucketId);
        serviceUtils.putNotEmpty(namedParameters, COL.CLASS_ID, classId);
        serviceUtils.putNotEmpty(namedParameters, COL.COLUMNS_ID, columnsId);
        serviceUtils.putNotEmpty(namedParameters, COL.FILTER_ID, filterId);
        serviceUtils.putNotEmpty(namedParameters, COL.DESCRIPTION, description);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(TAB.VIEW, true).insertIntoValues(namedParameters);

        try {
            jdbcTemplate.update(query.toString(logger, null), namedParameters, keyHolder, new String[]{COL.VIEW_ID});
            return keyHolder.getKey().intValue();
        } catch (BadSqlGrammarException e) {
            throw new UnexpectedException(e);
        }
    }

    public void deleteView(String userName, int viewId) throws UnknownColumnException, ConditionNotAllowedException {
        Condition condition = new Condition(COL.VIEW_ID, Operator.equal, viewId);

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(COL.DELETED, true);
        paramMap.put(COL.UPDATED_BY, userName);

        // Set view as deleted
        Query query = new Query(TAB.VIEW, true)
                .update()
                .set(paramMap)
                .where(condition, paramMap);

        jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
    }

    public void modifyView(String updatedBy, Integer viewId, String viewName, Integer bucketId, Integer classId, String description, Integer columnsId, Integer filterId) throws JsonProcessingException, UnknownColumnException, ItemDoNotExistsException, SQLException, ConditionNotAllowedException {
        if (serviceUtils.isViewExist(viewId)) {

            if (filterId != null && filterId > 0 && !serviceUtils.isFilterExist(filterId))
                throw new ItemDoNotExistsException("Filter", filterId);

            if (columnsId != null && columnsId > 0 && !serviceUtils.isColumnsExist(columnsId))
                throw new ItemDoNotExistsException("Columns", columnsId);

            Condition condition = new Condition(COL.VIEW_ID, Operator.equal, viewId);
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put(COL.UPDATED_BY, updatedBy);
            serviceUtils.putNotEmptyNullableInteger(paramMap, COL.BUCKET_ID, bucketId);
            serviceUtils.putNotEmptyNullableInteger(paramMap, COL.CLASS_ID, classId);
            serviceUtils.putNotEmptyNullableInteger(paramMap, COL.COLUMNS_ID, columnsId);
            serviceUtils.putNotEmptyNullableInteger(paramMap, COL.FILTER_ID, filterId);
            serviceUtils.putNotEmpty(paramMap, COL.VIEW_NAME, viewName);
            serviceUtils.putNotEmpty(paramMap, COL.DESCRIPTION, description);

            Query query = new Query(TAB.VIEW, false)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
        } else {
            throw new ItemDoNotExistsException("View", viewId);
        }
    }

    public Map<ResultField, Object> getViews(Optional<String> bucketName, Optional<Integer> viewId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnknownColumnException, ConditionNotAllowedException {
        Map<String, Object> paramMap = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (viewId.isPresent()) {
            conditions.add(new Condition(COL.VIEW_ID, Operator.equal, viewId.get()));
        } else if (bucketName.isPresent()) {
            int bucketId = serviceUtils.getBucketId(bucketName.get());
            conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));
        }

        if (urlConditions != null)
            conditions.addAll(urlConditions);

        Query queryCount = new Query(TAB.VIEW, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.VIEW, true)
                .select(serviceUtils.getViewColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<ResultField, Object> result = new HashMap<>();
        result.put(ResultField.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger, paramMap), paramMap, Long.TYPE));
        List<Map<String, Object>> viewsList = jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap);
        result.put(ResultField.DATA, viewsList);

        return result;
    }
}
