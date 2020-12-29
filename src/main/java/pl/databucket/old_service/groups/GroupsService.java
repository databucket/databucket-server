package pl.databucket.old_service.groups;

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


//@Service
public class GroupsService {

//    private final NamedParameterJdbcTemplate jdbcTemplate;
//    private final ServiceUtils serviceUtils;
//    Logger logger = LoggerFactory.getLogger(GroupsService.class);
//
//    public GroupsService(NamedParameterJdbcTemplate jdbcTemplate) {
//        this.jdbcTemplate = jdbcTemplate;
//        this.serviceUtils = new ServiceUtils(jdbcTemplate, logger);
//    }

//    public Integer createGroup(String userName, String groupName, String description, ArrayList<Integer> buckets) throws GroupAlreadyExistsException, ConditionNotAllowedException, UnknownColumnException, JsonProcessingException, SQLException {
//        if (serviceUtils.isGroupExist(groupName))
//            throw new GroupAlreadyExistsException(groupName);
//
//        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
//        namedParameters.addValue(COL.GROUP_NAME, groupName);
//        namedParameters.addValue(COL.CREATED_BY, userName);
//        serviceUtils.putNotEmpty(namedParameters, COL.DESCRIPTION, description);
//        serviceUtils.putNotEmpty(namedParameters, COL.BUCKETS, buckets);
//
//        KeyHolder keyHolder = new GeneratedKeyHolder();
//        Query query = new Query(TAB.GROUP, true).insertIntoValues(namedParameters);
//        jdbcTemplate.update(query.toString(logger, null), namedParameters, keyHolder, new String[]{COL.GROUP_ID});
//        return keyHolder.getKey().intValue();
//    }
//
//    public void deleteGroup(Integer groupId, String userName) throws UnknownColumnException, ConditionNotAllowedException {
//        Condition condition = new Condition(COL.GROUP_ID, Operator.equal, groupId);
//
//        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put(COL.DELETED, true);
//        paramMap.put(COL.UPDATED_BY, userName);
//
//        // Set group as deleted
//        Query query = new Query(TAB.GROUP, false)
//                .update()
//                .set(paramMap)
//                .where(condition, paramMap);
//
//        jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
//    }
//
//    public Map<ResultField, Object> getGroups(Optional<Integer> groupId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException, DataAccessException, UnexpectedException, ConditionNotAllowedException {
//        List<Condition> conditions = new ArrayList<>();
//        Map<String, Object> paramMap = new HashMap<>();
//
//        if (groupId.isPresent())
//            conditions.add(new Condition(COL.GROUP_ID, Operator.equal, groupId.get()));
//
//        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
//
//        if (urlConditions != null)
//            conditions.addAll(urlConditions);
//
//        Query queryCount = new Query(TAB.GROUP, true)
//                .selectCount()
//                .from()
//                .where(conditions, paramMap);
//
//        Query queryData = new Query(TAB.GROUP, true)
//                .select(serviceUtils.getGroupColumns())
//                .from()
//                .where(conditions, paramMap)
//                .orderBy(sort)
//                .limitPage(paramMap, limit, page);
//
//        Map<ResultField, Object> result = new HashMap<>();
//        result.put(ResultField.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger, paramMap), paramMap, Long.TYPE));
//        List<Map<String, Object>> groupList = jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap);
//        serviceUtils.convertBucketsToArray(groupList);
//        result.put(ResultField.DATA, groupList);
//
//        return result;
//    }
//
//    public void modifyGroup(String userName, Integer groupId, LinkedHashMap<String, Object> body) throws ItemDoNotExistsException, GroupAlreadyExistsException, JsonProcessingException, DataAccessException, UnknownColumnException, SQLException, ConditionNotAllowedException {
//        if (serviceUtils.isGroupExist(groupId)) {
//
//            if (body.containsKey(COL.GROUP_NAME)) {
//                String newGroupName = (String) body.get(COL.GROUP_NAME);
//
//                if (serviceUtils.isGroupExist(groupId, newGroupName))
//                    throw new GroupAlreadyExistsException(newGroupName);
//            }
//
//            Condition condition = new Condition(COL.GROUP_ID, Operator.equal, groupId);
//            Map<String, Object> paramMap = new HashMap<>();
//            paramMap.put(COL.UPDATED_BY, userName);
//            serviceUtils.putNotEmpty(body, paramMap, COL.GROUP_NAME);
//            serviceUtils.putNotEmpty(body, paramMap, COL.DESCRIPTION);
//            serviceUtils.putNotEmpty(body, paramMap, COL.BUCKETS);
//
//            Query query = new Query(TAB.GROUP, false)
//                    .update()
//                    .set(paramMap)
//                    .where(condition, paramMap);
//
//            this.jdbcTemplate.update(query.toString(logger, paramMap), paramMap);
//
//        } else {
//            throw new ItemDoNotExistsException("Group", groupId);
//        }
//    }
}
