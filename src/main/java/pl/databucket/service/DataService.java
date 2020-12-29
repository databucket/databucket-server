package pl.databucket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DeadlockLoserDataAccessException;
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
import pl.databucket.old_service.ServiceUtils;
import pl.databucket.database.*;
import pl.databucket.exception.ConditionNotAllowedException;
import pl.databucket.exception.ItemDoNotExistsException;
import pl.databucket.exception.UnexpectedException;
import pl.databucket.exception.UnknownColumnException;

import java.io.IOException;
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
        this.serviceUtils = new ServiceUtils(jdbcTemplate, logger);
    }

    public long getDataCount(String bucketName, Query query, Map<String, Object> namedParameters) throws ItemDoNotExistsException, UnexpectedException {
        try {
            return jdbcTemplate.queryForObject(query.toString(logger, namedParameters), namedParameters, Long.TYPE);
        } catch (Exception e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }

    public List<Integer> lockData(String bucketName, String userName, Optional<Integer[]> tagId, Optional<Integer> filterId, Optional<List<Condition>> conditions, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException, ConditionNotAllowedException {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);

        Map<String, Object> paramMap = new HashMap<>();
        List<Condition> selectConditions = new ArrayList<>();

        if (filterId.isPresent()) {
            selectConditions = serviceUtils.getFilterConditions(filterId.get());
        } else if (tagId.isPresent()) {
            selectConditions.add(new Condition(COL.TAG_ID, Operator.in, new ArrayList<>(Arrays.asList(tagId.get()))));
        } else if (conditions.isPresent()) {
            selectConditions = conditions.get();

        }

        boolean joinTags = false;
        if (sort.isPresent())
            joinTags = sort.get().contains(COL.TAG_NAME);
        joinTags = joinTags || serviceUtils.usedTagNameColumn(selectConditions);

        selectConditions.add(new Condition(COL.LOCKED, Operator.equal, false));

        Query selectQuery = new Query(bucketName, true)
                .select(COL.DATA_ID)
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
                        List<Integer> listOfIds = jdbcTemplate.queryForList(selectQuery.toString(logger, paramMap), paramMap, Integer.class);
                        if (listOfIds.size() > 0) {
                            Condition condition = new Condition(COL.DATA_ID, Operator.in, listOfIds);

                            Map<String, Object> updateNamedParams = new HashMap<>();
                            updateNamedParams.put(COL.LOCKED_BY, userName);
                            updateNamedParams.put(COL.UPDATED_BY, userName);
                            updateNamedParams.put(COL.LOCKED, true);

                            Query updateQuery = null;
                            try {
                                updateQuery = new Query(bucketName, false)
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
                    throw new UnknownColumnException(serviceUtils.getColumnName(e.getMessage()));
                else
                    throw new UnexpectedException(e);
            }
        }

        return bundlesIdsList;
    }

    public List<Map<String, Object>> getDataByQuery(String bucketName, Query query, Map<String, Object> namedParameters) throws ItemDoNotExistsException, UnknownColumnException, UnexpectedException {
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(query.toString(logger, namedParameters), namedParameters);
            serviceUtils.convertStringToMap(result, COL.PROPERTIES);
            return result;
        } catch (BadSqlGrammarException e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else if (e.getMessage().contains("Unknown column"))
                throw new UnknownColumnException(serviceUtils.getColumnName(e.getMessage()));
            else
                throw new UnexpectedException(e);
        } catch (Exception ee) {
            throw new UnexpectedException(ee);
        }
    }

    private List<Map<String, Object>> getDataDefaultColumns() {
        List<Map<String, Object>> columns = new ArrayList<>();
        final String[] staticColumns = {COL.DATA_ID, COL.TAG_ID, COL.TAG_NAME, COL.LOCKED, COL.LOCKED_BY, COL.PROPERTIES + "::varchar", COL.CREATED_AT, COL.CREATED_BY, COL.UPDATED_AT, COL.UPDATED_BY};
        for (String colName : staticColumns) {
            Map<String, Object> colMap = new HashMap<>();
            colMap.put(C.FIELD, colName);
            columns.add(colMap);
        }
        return columns;
    }

    public Map<ResultField, Object> getData(String bucketName, Optional<Integer[]> dataId, Optional<Integer[]> tagId, Optional<Integer> filterId, Optional<Integer> viewId, Optional<List<Map<String, Object>>> inColumns, Optional<List<Condition>> inConditions, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort) throws ItemDoNotExistsException, UnknownColumnException, UnexpectedException, ConditionNotAllowedException {

        List<Map<String, Object>> columns;
        Map<String, Object> paramMap = new HashMap<>();
        List<Condition> conditions = new ArrayList<>();

        Query queryData;
        Query queryCount;

        boolean joinTags = false;
        if (sort.isPresent())
            joinTags = sort.get().contains(COL.TAG_NAME);

        // ---------- ID ------------
        if (dataId.isPresent()) {
            columns = getDataDefaultColumns();
            joinTags = true; // currently default columns contain tag_name
            conditions.add(new Condition(COL.DATA_ID, Operator.in, new ArrayList<>(Arrays.asList(dataId.get()))));

            // ---------- FILTER ------------
        } else if (filterId.isPresent()) {
            columns = getDataDefaultColumns();
            joinTags = true; // currently default columns contain tag_name
            conditions = serviceUtils.getFilterConditions(filterId.get());

            // ---------- TAG ------------
        } else if (tagId.isPresent()) {
            columns = getDataDefaultColumns();
            joinTags = true; // currently default columns contain tag_name
            conditions.add(new Condition(COL.TAG_ID, Operator.in, new ArrayList<>(Arrays.asList(tagId.get()))));

            // ---------- VIEW ------------
        } else if (viewId.isPresent()) {

            Map<String, Object> view = serviceUtils.getView(viewId.get());

            // Prepare columns
            boolean isBundleId = false;
            columns = (List<Map<String, Object>>) view.get(COL.COLUMNS);
            for (Map<String, Object> column : columns) {
                String field = (String) column.get(C.FIELD);

                if (field.equals(COL.TAG_NAME))
                    joinTags = true;

                if (field.equals(COL.DATA_ID))
                    isBundleId = true;
            }

            if (!isBundleId) {
                Map<String, Object> colBundleId = new HashMap<>();
                colBundleId.put(C.FIELD, COL.DATA_ID);
                columns.add(colBundleId);
            }

            // Filter conditions
            if (view.containsKey(COL.FILTER_ID) && view.get(COL.FILTER_ID) != null) {
                conditions = serviceUtils.getFilterConditions((int) view.get(COL.FILTER_ID));
            }

            // View conditions
            if (view.containsKey(COL.CONDITIONS) && view.get(COL.CONDITIONS) != null) {
                List<Condition> viewConditions = (List<Condition>) view.get(COL.CONDITIONS);
                conditions.addAll(viewConditions);
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

                    if (field.equals(COL.DATA_ID))
                        isBundleId = true;
                }

                columns = inColumns.get();

                if (!isBundleId) {
                    Map<String, Object> colBundleId = new HashMap<>();
                    colBundleId.put(C.FIELD, COL.DATA_ID);
                    columns.add(colBundleId);
                }
            } else {
                columns = getDataDefaultColumns();
                joinTags = true;
            }

            if (inConditions.isPresent())
                conditions = inConditions.get();
        }

        joinTags = joinTags || serviceUtils.usedTagNameColumn(conditions);

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

        long count = getDataCount(bucketName, queryCount, paramMap);
        List<Map<String, Object>> data = getDataByQuery(bucketName, queryData, paramMap);

        // Data from properties are always as strings. This converts them into proper types.
        serviceUtils.convertPropertiesColumns(data);

        Map<ResultField, Object> result = new HashMap<>();
        result.put(ResultField.TOTAL, count);
        result.put(ResultField.DATA, data);
        return result;
    }

    public int createData(String createdBy, String bucketName, Map<String, Object> details) throws JsonProcessingException, UnexpectedException, ItemDoNotExistsException, UnknownColumnException, ConditionNotAllowedException {
        try {
            MapSqlParameterSource namedParameters = new MapSqlParameterSource();
            if (details.containsKey(COL.TAG_ID))
                namedParameters.addValue(COL.TAG_ID, details.get(COL.TAG_ID));
            else if (details.containsKey(COL.TAG_NAME)) {
                String tagName = (String) details.get(COL.TAG_NAME);
                namedParameters.addValue(COL.TAG_ID, serviceUtils.getTagId(tagName));
            }

            namedParameters.addValue(COL.CREATED_BY, createdBy);
            boolean added = serviceUtils.putNotEmpty(namedParameters, COL.LOCKED, details.get(COL.LOCKED));
            if (added && (boolean) details.get(COL.LOCKED))
                namedParameters.addValue(COL.LOCKED_BY, createdBy);
            serviceUtils.putNotEmpty(namedParameters, COL.PROPERTIES, details.get(COL.PROPERTIES));

            KeyHolder keyHolder = new GeneratedKeyHolder();
            Query query = new Query(bucketName, true).insertIntoValues(namedParameters);
            jdbcTemplate.update(query.toString(logger, namedParameters.getValues()), namedParameters, keyHolder, new String[]{COL.DATA_ID});
            return keyHolder.getKey().intValue();
        } catch (BadSqlGrammarException | SQLException e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }


    public int modifyData(String updatedBy, String bucketName, Optional<Integer[]> dataIdArray, Optional<Integer> filterId, Optional<Integer[]> tagsIds, LinkedHashMap<String, Object> details) throws IOException, UnexpectedException, ItemDoNotExistsException, UnknownColumnException, SQLException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        if (dataIdArray.isPresent()) {
            conditions.add(new Condition(COL.DATA_ID, Operator.in, new ArrayList<>(Arrays.asList(dataIdArray.get()))));
        } else if (filterId.isPresent()) {
            conditions = serviceUtils.getFilterConditions(filterId.get());
        } else if (tagsIds.isPresent()) {
            conditions.add(new Condition(COL.TAG_ID, Operator.in, new ArrayList<>(Arrays.asList(tagsIds.get()))));
        } else if (details.get(COL.CONDITIONS) != null) {
            List<Map<String, Object>> bodyConditions = (List<Map<String, Object>>) details.get(COL.CONDITIONS);
            for (Map<String, Object> bodyCondition : bodyConditions)
                conditions.add(new Condition(bodyCondition));
        }

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put(COL.UPDATED_BY, updatedBy);

        if (details.containsKey(COL.TAG_ID))
            serviceUtils.putNotEmpty(details, paramMap, COL.TAG_ID);
        else if (details.containsKey(COL.TAG_NAME)) {
            int tagId = serviceUtils.getTagId((String) details.get(COL.TAG_NAME));
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

        boolean properties = serviceUtils.putNotEmpty(details, paramMap, COL.PROPERTIES);
        boolean joinTags = serviceUtils.usedTagNameColumn(conditions);

        Query query = new Query(bucketName, false)
                .update()
                .joinTags(joinTags)
                .set(paramMap)
                .removeAndSetProperties(!properties, details)
                .where(conditions, paramMap);

        try {
            return this.jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
        } catch (Exception e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }

    public int deleteData(String bucketName, Optional<Integer[]> bundlesIds, Optional<Integer> filterId, Optional<Integer[]> tagsIds) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        Map<String, Object> paramMap = new HashMap<>();

        if (bundlesIds.isPresent()) {
            conditions.add(new Condition(COL.DATA_ID, Operator.in, new ArrayList<>(Arrays.asList(bundlesIds.get()))));
        } else if (filterId.isPresent()) {
            conditions = serviceUtils.getFilterConditions(filterId.get());
        } else if (tagsIds.isPresent()) {
            conditions.add(new Condition(COL.TAG_ID, Operator.in, new ArrayList<>(Arrays.asList(tagsIds.get()))));
        }

        Query query = new Query(bucketName, false)
                .delete()
                .from()
                .where(conditions, paramMap);

        try {
            return jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
        } catch (Exception e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }

    public int deleteData(String bucketName, List<Condition> conditions) throws ItemDoNotExistsException, UnknownColumnException, UnexpectedException, ConditionNotAllowedException {
        Map<String, Object> paramMap = new HashMap<>();

        boolean joinTags = serviceUtils.usedTagNameColumn(conditions);

        Query query = new Query(bucketName, true)
                .delete()
                .from()
                .joinTags(joinTags)
                .where(conditions, paramMap);

        try {
            return jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
        } catch (Exception e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }

    public List<Map<String, Object>> getDataHistory(String bucketName, Integer dataId) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException, ConditionNotAllowedException {
        Map<String, Object> paramMap = new HashMap<>();

        String[] columns = {COL.ID, COL.TAG_ID, COL.LOCKED, COL.PROPERTIES + " is not null as \"" + COL.PROPERTIES + "\"", COL.UPDATED_AT, COL.UPDATED_BY};
        Condition condition = new Condition(COL.DATA_ID, Operator.equal, dataId);

        Query query = new Query(bucketName + "_history", true)
                .select(columns)
                .from()
                .where(condition, paramMap)
                .orderBy("a." + COL.UPDATED_AT, true);
        try {
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

    public List<Map<String, Object>> getDataHistoryProperties(String bucketName, Integer dataId, Integer[] ids) throws ItemDoNotExistsException, UnexpectedException, UnknownColumnException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        Map<String, Object> namedParameters = new HashMap<>();

        String[] columns = {COL.ID, COL.PROPERTIES + "::varchar"};

        conditions.add(new Condition(COL.DATA_ID, Operator.equal, dataId));
        conditions.add(new Condition(COL.ID, Operator.in, new ArrayList<>(Arrays.asList(ids))));

        Query query = new Query(bucketName + "_history", true)
                .select(columns)
                .from()
                .where(conditions, namedParameters)
                .orderBy(COL.ID, true);
        try {
            List<Map<String, Object>> result = jdbcTemplate.queryForList(query.toString(logger, namedParameters), namedParameters);
            serviceUtils.convertStringToMap(result, COL.PROPERTIES);
            return result;
        } catch (Exception e) {
            if (e.getMessage().matches(".*Table '.*' doesn't exist.*"))
                throw new ItemDoNotExistsException("Bucket", bucketName);
            else
                throw new UnexpectedException(e);
        }
    }


}
