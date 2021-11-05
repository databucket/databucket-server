package pl.databucket.server.service.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import pl.databucket.server.dto.*;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ConditionNotAllowedException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.UnexpectedException;
import pl.databucket.server.exception.UnknownColumnException;
import pl.databucket.server.mapper.DataRowMapper;
import pl.databucket.server.service.ServiceUtils;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;


@Service
public class DataService {

    private final PlatformTransactionManager transactionManager;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ServiceUtils serviceUtils;
    Logger logger = LoggerFactory.getLogger(DataService.class);

    public DataService(NamedParameterJdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionManager = transactionManager;
        this.serviceUtils = new ServiceUtils();
    }


    public DataDTO createData(User user, Bucket bucket, DataCreateDTO dataCreateDto) throws JsonProcessingException, UnexpectedException, ItemNotFoundException, UnknownColumnException, ConditionNotAllowedException, SQLException {

        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        if (dataCreateDto.getTagId() != null)
            paramMap.addValue(COL.TAG_ID, dataCreateDto.getTagId());

        paramMap.addValue(COL.CREATED_BY, user.getUsername());
        paramMap.addValue(COL.MODIFIED_BY, user.getUsername());
        if (dataCreateDto.getReserved() != null) {
            paramMap.addValue(COL.RESERVED, dataCreateDto.getReserved());
            if (dataCreateDto.getReserved())
                paramMap.addValue(COL.RESERVED_BY, user.getUsername());
        }
        paramMap.addValue(COL.PROPERTIES, serviceUtils.javaObjectToPGObject(dataCreateDto.getProperties()));

        KeyHolder keyHolder = new GeneratedKeyHolder();
        Query query = new Query(bucket.getTableName()).insertIntoValues(paramMap);
        jdbcTemplate.update(query.toString(logger, paramMap.getValues()), paramMap, keyHolder, new String[]{COL.DATA_ID});
        long id = Objects.requireNonNull(keyHolder.getKey()).intValue();
        return getData(user, bucket, id);
    }

    public void createData(User user, Bucket bucket, List<DataCreateDTO> dataList) {

        String sql = "INSERT INTO \"" + bucket.getTableName() + "\" (\"tag_id\", \"created_by\", \"modified_by\", \"reserved\", \"reserved_by\", \"properties\") VALUES (?, ?, ?, ?, ?, ?)";
        logger.debug("create " + dataList.size() + " data rows");
        logger.debug(sql);
        int[] result = jdbcTemplate.getJdbcTemplate().batchUpdate(sql, new BatchPreparedStatementSetter() {
            @SneakyThrows
            @Override
            public void setValues(PreparedStatement ps, int i) {
                DataCreateDTO dataCreateDTO = dataList.get(i);
                ps.setLong(1, dataCreateDTO.getTagId());
                ps.setString(2, user.getUsername());
                ps.setString(3, user.getUsername());
                ps.setBoolean(4, dataCreateDTO.getReserved());
                ps.setString(5, dataCreateDTO.getReserved() ? dataCreateDTO.getOwner() != null ? dataCreateDTO.getOwner() : user.getUsername() : null);
                ps.setObject(6, serviceUtils.javaObjectToPGObject(dataCreateDTO.getProperties()));
            }

            @Override
            public int getBatchSize() {
                return dataList.size();
            }
        });
    }

    public DataDTO getData(User user, Bucket bucket, long id) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.DATA_ID, Operator.equal, id));

        if (bucket.isProtectedData() && !user.isAdminUser())
            conditions.add(new Condition(COL.RESERVED_BY, Operator.equal, user.getUsername()));

        Map<String, Object> paramMap = new HashMap<>();
        Query queryData = new Query(bucket.getTableName())
                .select("*")
                .from()
                .where(conditions, paramMap);

        return jdbcTemplate.queryForObject(queryData.toString(logger, paramMap), paramMap, new DataRowMapper());
    }

    public List<DataDTO> getData(User user, Bucket bucket, List<Long> ids) throws UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.DATA_ID, Operator.in, ids));

        if (bucket.isProtectedData() && !user.isAdminUser())
            conditions.add(new Condition(COL.RESERVED_BY, Operator.equal, user.getUsername()));

        Map<String, Object> paramMap = new HashMap<>();
        Query queryData = new Query(bucket.getTableName())
                .select("*")
                .from()
                .where(conditions, paramMap);

        return jdbcTemplate.query(queryData.toString(logger, paramMap), paramMap, new DataRowMapper());
    }

    public Map<ResultField, Object> getData(User user, Bucket bucket, Optional<List<CustomColumnDto>> inColumns, QueryRule queryRule, Integer page, Integer limit, String sort) throws ItemNotFoundException, UnknownColumnException, UnexpectedException, ConditionNotAllowedException {

        List<CustomColumnDto> columns = null;
        Map<String, Object> paramMap = new HashMap<>();

        // prepare custom columns
        if (inColumns.isPresent()) {
            columns = inColumns.get();

            boolean isDataId = false;
            for (CustomColumnDto column : columns)
                if (column.getField().equals(COL.DATA_ID)) {
                    isDataId = true;
                    break;
                }

            if (!isDataId) {
                CustomColumnDto colDataId = new CustomColumnDto();
                colDataId.setField(COL.DATA_ID);
                colDataId.setTitle("Id");
                columns.add(colDataId);
            }
        }

        if (bucket.isProtectedData() && !user.isAdminUser())
            queryRule.getConditions().add(new Condition(COL.RESERVED_BY, Operator.equal, user.getUsername()));

        Query queryCount = new Query(bucket.getTableName())
                .select(COL.COUNT)
                .from()
                .where(queryRule, paramMap);

        long count = jdbcTemplate.queryForObject(queryCount.toString(logger, paramMap), paramMap, Long.TYPE);
        Map<ResultField, Object> result = new HashMap<>();
        result.put(ResultField.TOTAL, count);

        if (limit > 0) {
            Query queryData = new Query(bucket.getTableName())
                    .selectData(columns)
                    .from()
                    .where(queryRule, paramMap)
                    .orderBy(sort)
                    .limitPage(paramMap, limit, page);

            if (inColumns.isPresent()) {
                List<Map<String, Object>> dataList = jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap);
                serviceUtils.convertPropertiesColumns(dataList);
                result.put(ResultField.CUSTOM_DATA, dataList);
            } else {
                List<DataDTO> dataList = jdbcTemplate.query(queryData.toString(logger, paramMap), paramMap, new DataRowMapper());
                result.put(ResultField.DATA, dataList);
            }
        }

        return result;
    }

    public String getQuery(QueryRule queryRule) throws UnknownColumnException, ConditionNotAllowedException {
        Map<String, Object> paramMap = new HashMap<>();
        Query selectQuery = new Query("bucket")
                .select(COL.COUNT)
                .from()
                .where(queryRule, paramMap);
        return selectQuery.toString(logger, paramMap);
    }

    public int modifyData(User user, Bucket bucket, Optional<List<Long>> dataIdArray, DataModifyDTO dataModifyDto, QueryRule queryRule) throws IOException, UnexpectedException, ItemNotFoundException, UnknownColumnException, SQLException, ConditionNotAllowedException {

        if (dataIdArray.isPresent()) {
            queryRule.getConditions().add(new Condition(COL.DATA_ID, Operator.in, dataIdArray.get()));
        } else if (dataModifyDto.getConditions() != null) {
            List<Map<String, Object>> bodyConditions = dataModifyDto.getConditions();
            for (Map<String, Object> bodyCondition : bodyConditions)
                queryRule.getConditions().add(new Condition(bodyCondition));
        }

        if (bucket.isProtectedData() && !user.isAdminUser())
            queryRule.getConditions().add(new Condition(COL.RESERVED_BY, Operator.equal, user.getUsername()));

        Map<String, Object> paramMap = new HashMap<>();

        if (dataModifyDto.getTagId() != null)
            paramMap.put(COL.TAG_ID, dataModifyDto.getTagId());

        paramMap.put(COL.MODIFIED_BY, user.getUsername());
        paramMap.put(COL.MODIFIED_AT, new java.sql.Timestamp(new java.util.Date().getTime()));

        if (dataModifyDto.getReserved() != null) {
            paramMap.put(COL.RESERVED, dataModifyDto.getReserved());
            if (dataModifyDto.getReserved())
                paramMap.put(COL.RESERVED_BY, user.getUsername());
            else
                paramMap.put(COL.RESERVED_BY, null);
        }

        boolean properties = false;
        if (dataModifyDto.getProperties() != null) {
            paramMap.put(COL.PROPERTIES, serviceUtils.javaObjectToPGObject(dataModifyDto.getProperties()));
            properties = true;
        }

        Query query = new Query(bucket.getTableName())
                .update()
                .set(paramMap)
                .removeAndSetProperties(!properties, dataModifyDto)
                .where(queryRule, paramMap);

        return this.jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
    }

    public List<Long> reserveData(User user, Bucket bucket, QueryRule queryRule, Integer limit, String sort, String targetOwnerUsername) throws UnknownColumnException, ConditionNotAllowedException, UnexpectedException {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

        Map<String, Object> paramMap = new HashMap<>();
        queryRule.getConditions().add(new Condition(COL.RESERVED, Operator.equal, false));

        Query selectQuery = new Query(bucket.getTableName())
                .select(COL.DATA_ID)
                .from()
                .where(queryRule, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, 1);

        boolean done = false;
        int tryCount = 7;
        List<Long> dataIds = null;

        while (!done && tryCount > 0) {
            try {
                dataIds = transactionTemplate.execute(paramTransactionStatus -> {
                    List<Long> listOfIds = jdbcTemplate.queryForList(selectQuery.toString(logger, paramMap), paramMap, Long.class);
                    if (listOfIds.size() > 0) {
                        Condition condition = new Condition(COL.DATA_ID, Operator.in, listOfIds);

                        Map<String, Object> updateNamedParams = new HashMap<>();
                        updateNamedParams.put(COL.RESERVED_BY, targetOwnerUsername);
                        updateNamedParams.put(COL.MODIFIED_BY, user.getUsername());
                        updateNamedParams.put(COL.RESERVED, true);

                        Query updateQuery = null;
                        try {
                            updateQuery = new Query(bucket.getTableName())
                                    .update()
                                    .set(updateNamedParams)
                                    .where(condition, updateNamedParams);
                        } catch (UnknownColumnException | ConditionNotAllowedException e) {
                            e.printStackTrace();
                        }

                        jdbcTemplate.update(updateQuery.toString(logger, updateNamedParams), updateNamedParams);
                        return listOfIds;
                    } else
                        return null;
                });

                done = true;
            } catch (DeadlockLoserDataAccessException | CannotSerializeTransactionException | JpaSystemException d) {
                tryCount -= 1;
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                throw new UnexpectedException(e);
            }
        }

        return dataIds;
    }

    public int deleteDataByIds(User user, Bucket bucket, List<Long> dataIds) throws UnknownColumnException, ConditionNotAllowedException {
        Map<String, Object> paramMap = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();
        conditions.add(new Condition(COL.DATA_ID, Operator.in, dataIds));

        if (bucket.isProtectedData() && !user.isAdminUser())
            conditions.add(new Condition(COL.RESERVED_BY, Operator.equal, user.getUsername()));

        Query query = new Query(bucket.getTableName())
                .delete()
                .from()
                .where(conditions, paramMap);

        return jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
    }

    public int deleteDataByRules(User user, Bucket bucket, QueryRule queryRule) throws UnknownColumnException, ConditionNotAllowedException {
        Map<String, Object> paramMap = new HashMap<>();

        if (bucket.isProtectedData() && !user.isAdminUser())
            queryRule.getConditions().add(new Condition(COL.RESERVED_BY, Operator.equal, user.getUsername()));

        Query query = new Query(bucket.getTableName())
                .delete()
                .from()
                .where(queryRule, paramMap);

        return jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
    }

    public List<Map<String, Object>> getDataHistory(Bucket bucket, Long dataId) throws UnknownColumnException, ConditionNotAllowedException {
        Map<String, Object> paramMap = new HashMap<>();

        String[] columns = {COL.ID, COL.TAG_ID, COL.RESERVED, COL.PROPERTIES + " is not null as \"" + COL.PROPERTIES + "\"", COL.MODIFIED_AT, COL.MODIFIED_BY};
        Condition condition = new Condition(COL.DATA_ID, Operator.equal, dataId);

        Query query = new Query(bucket.getTableHistoryName())
                .select(columns)
                .from()
                .where(condition, paramMap)
                .orderBy(COL.MODIFIED_AT, true);

        List<Map<String, Object>> result = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);

        // clean null properties
        for (Map<String, Object> map : result) {

            // remove null properties items, change value into boolean
            if (!(Boolean) map.get(COL.PROPERTIES)) {
                map.remove(COL.PROPERTIES);
            } else {
                map.replace(COL.PROPERTIES, true); // true means -> properties has been changed
            }

            // remove null tag_id items if previous tag_id is the same
            if (map.get(COL.TAG_ID) == null) {
                map.remove(COL.TAG_ID);
            }

            // remove null locked items
            if (map.get(COL.RESERVED) == null) {
                map.remove(COL.RESERVED);
            }
        }

        return result;
    }

    public List<Map<String, Object>> getDataHistoryProperties(Bucket bucket, Long dataId, List<Long> ids) throws ItemNotFoundException, UnexpectedException, UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        Map<String, Object> namedParameters = new HashMap<>();

        String[] columns = {COL.ID, COL.PROPERTIES + "::varchar"};

        conditions.add(new Condition(COL.DATA_ID, Operator.equal, dataId));
        conditions.add(new Condition(COL.ID, Operator.in, ids));

        Query query = new Query(bucket.getTableHistoryName())
                .select(columns)
                .from()
                .where(conditions, namedParameters)
                .orderBy(COL.ID, true);

        List<Map<String, Object>> result = jdbcTemplate.queryForList(query.toString(logger, namedParameters), namedParameters);
        serviceUtils.convertStringToMap(result, COL.PROPERTIES);
        return result;
    }
}
