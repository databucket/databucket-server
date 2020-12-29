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
public class ColumnService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    Logger logger = LoggerFactory.getLogger(ColumnService.class);
    private final ServiceUtils serviceUtils;

    public ColumnService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.serviceUtils = new ServiceUtils(jdbcTemplate, logger);
    }

    public int createColumns(String columnsName, Integer bucketId, String createdBy, List<Map<String, Object>> columns, String description, Integer classId) throws JsonProcessingException, ColumnsAlreadyExistsException, UnknownColumnException, SQLException, ConditionNotAllowedException {

        if (columnsName != null) {
            if (serviceUtils.isColumnsExist(columnsName))
                throw new ColumnsAlreadyExistsException(columnsName);
        }

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue(COL.COLUMNS_NAME, columnsName);
        namedParameters.addValue(COL.CREATED_BY, createdBy);
        namedParameters.addValue(COL.BUCKET_ID, bucketId);
        namedParameters.addValue(COL.CLASS_ID, classId);
        namedParameters.addValue(COL.COLUMNS, serviceUtils.javaObjectToPGObject(columns));
        serviceUtils.putNotEmpty(namedParameters, COL.DESCRIPTION, description);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(TAB.COLUMNS, true).insertIntoValues(namedParameters);
        jdbcTemplate.update(query.toString(logger, null), namedParameters, keyHolder, new String[]{COL.COLUMNS_ID});
        return keyHolder.getKey().intValue();
    }

    public Map<String, Object> getColumns(Optional<String> bucketName, Optional<Integer> columnsId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException, ConditionNotAllowedException {
        Map<String, Object> paramMap = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (columnsId.isPresent()) {
            conditions.add(new Condition(COL.COLUMNS_ID, Operator.equal, columnsId.get()));
        } else if (bucketName.isPresent()) {
            int bucketId = serviceUtils.getBucketId(bucketName.get());
            conditions.add(new Condition(COL.BUCKET_ID, Operator.equal, bucketId));
        }

        if (urlConditions != null)
            conditions.addAll(urlConditions);

        Query queryCount = new Query(TAB.COLUMNS, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.COLUMNS, true)
                .select(serviceUtils.getColumnsColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<String, Object> result = new HashMap<>();
        result.put(C.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger, paramMap), paramMap, Long.TYPE));
        List<Map<String, Object>> columnsList = jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap);
        serviceUtils.convertStringToList(columnsList, COL.COLUMNS);
        result.put(C.COLUMNS, columnsList);

        return result;
    }

    public void modifyColumns(String updatedBy, Integer columnsId, String columnsName, Integer bucketId, Integer classId, String description, List<Map<String, Object>> columns) throws ItemDoNotExistsException, UnexpectedException, ColumnsAlreadyExistsException, UnknownColumnException, ConditionNotAllowedException {

        if (serviceUtils.isColumnsExist(columnsId)) {
            if (columnsName != null) {
                if (serviceUtils.isColumnsExist(columnsName))
                    throw new ColumnsAlreadyExistsException(columnsName);
            }

            Condition condition = new Condition(COL.COLUMNS_ID, Operator.equal, columnsId);
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put(COL.UPDATED_BY, updatedBy);
            try {
                serviceUtils.putNotEmptyNullableInteger(paramMap, COL.BUCKET_ID, bucketId);
                serviceUtils.putNotEmptyNullableInteger(paramMap, COL.CLASS_ID, classId);
                serviceUtils.putNotEmpty(paramMap, COL.COLUMNS_NAME, columnsName);
                serviceUtils.putNotEmpty(paramMap, COL.DESCRIPTION, description);
                serviceUtils.putNotEmpty(paramMap, COL.COLUMNS, columns);
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }


            Query query = new Query(TAB.COLUMNS, false)
                    .update()
                    .set(paramMap)
                    .where(condition, paramMap);

            this.jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
        } else {
            throw new ItemDoNotExistsException("Columns", columnsId);
        }
    }

    public void deleteColumns(String userName, int columnsId) throws ItemDoNotExistsException, ItemAlreadyUsedException, UnknownColumnException, ConditionNotAllowedException {
        if (serviceUtils.isColumnsExist(columnsId)) {

            String message = referencesColumnsItem(columnsId);
            if (message != null)
                throw new ItemAlreadyUsedException(message);

            Condition condition = new Condition(COL.COLUMNS_ID, Operator.equal, columnsId);

            Map<String, Object> namedParameters = new HashMap<>();
            namedParameters.put(COL.DELETED, true);
            namedParameters.put(COL.UPDATED_BY, userName);

            // Set columns as deleted
            Query query = new Query(TAB.COLUMNS, false)
                    .update()
                    .set(namedParameters)
                    .where(condition, namedParameters);

            jdbcTemplate.update(query.toString(logger, namedParameters), namedParameters);
        } else
            throw new ItemDoNotExistsException("Columns", columnsId);
    }

    private String referencesColumnsItem(int columnsId) throws UnknownColumnException, ConditionNotAllowedException {
        final String AS_ID = " as \"id\"";
        final String AS_NAME = " as \"name\"";
        String result = "";

        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.COLUMNS_ID, Operator.equal, columnsId));
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
