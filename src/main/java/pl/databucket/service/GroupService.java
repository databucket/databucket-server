package pl.databucket.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import pl.databucket.database.*;
import pl.databucket.model.beans.GroupBean;
import pl.databucket.model.entity.Group;
import pl.databucket.exception.*;
import pl.databucket.old_service.ServiceUtils;
import pl.databucket.repository.group.GroupRepository;

import java.sql.SQLException;
import java.util.*;


@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ServiceUtils serviceUtils;
    Logger logger = LoggerFactory.getLogger(GroupService.class);

    public GroupService(GroupRepository groupRep, NamedParameterJdbcTemplate jdbcTemplate) {
        this.groupRepository = groupRep;
        this.jdbcTemplate = jdbcTemplate;
        this.serviceUtils = new ServiceUtils(jdbcTemplate, logger);
    }

    public Group createGroup(GroupBean groupBean) throws GroupAlreadyExistsException {
        if (groupRepository.existsByName(groupBean.getName()))
            throw new GroupAlreadyExistsException(groupBean.getName());

        Group group = new Group();
        group.setName(groupBean.getName());
        group.setDescription(groupBean.getDescription());
        group.setBuckets(groupBean.getBuckets());

        return groupRepository.save(group);
    }

    public void deleteGroup(Integer groupId, String userName) {
        Group group = groupRepository.getOne(groupId);
        group.setDeleted(true);
//        group.setUpdatedBy(userName);
        groupRepository.save(group);
    }

//    public Map<ResultField, Object> getGroups(Optional<Integer> groupId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException, DataAccessException, UnexpectedException, ConditionNotAllowedException {
//        List<Condition> conditions = new ArrayList<>();
//        if (groupId.isPresent())
//            conditions.add(new Condition(COL.GROUP_ID, Operator.equal, groupId.get()));
//        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
//        if (urlConditions != null)
//            conditions.addAll(urlConditions);
//
//        List<Predicate> predicates = groupRepository.createPredicates(conditions);
//        Order orderBy = groupRepository.createOrder(sort.get());
//        int offset = page.get() * limit.get() - limit.get();
//
//        int total = groupRepository.countByCustomCriteria(predicates);
//        List<Group> groupList = groupRepository.findByCustomCriteria(predicates, orderBy, offset, limit.get());
//
//        Map<ResultField, Object> result = new HashMap<>();
//        result.put(ResultField.TOTAL, total);
//        result.put(ResultField.DATA, groupList);
//
//        return result;
//    }

    public Map<ResultField, Object> getGroups(Optional<Integer> groupId, Optional<Integer> page, Optional<Integer> limit, Optional<String> sort, List<Condition> urlConditions) throws UnknownColumnException, DataAccessException, UnexpectedException, ConditionNotAllowedException {
        List<Condition> conditions = new ArrayList<>();
        Map<String, Object> paramMap = new HashMap<>();

        if (groupId.isPresent())
            conditions.add(new Condition(COL.GROUP_ID, Operator.equal, groupId.get()));

        conditions.add(new Condition(COL.DELETED, Operator.equal, false));

        if (urlConditions != null)
            conditions.addAll(urlConditions);

        Query queryCount = new Query(TAB.GROUP, true)
                .selectCount()
                .from()
                .where(conditions, paramMap);

        Query queryData = new Query(TAB.GROUP, true)
                .select(serviceUtils.getGroupColumns())
                .from()
                .where(conditions, paramMap)
                .orderBy(sort)
                .limitPage(paramMap, limit, page);

        Map<ResultField, Object> result = new HashMap<>();
        result.put(ResultField.TOTAL, jdbcTemplate.queryForObject(queryCount.toString(logger, paramMap), paramMap, Long.TYPE));
        List<Map<String, Object>> groupList = jdbcTemplate.queryForList(queryData.toString(logger, paramMap), paramMap);
        serviceUtils.convertBucketsToArray(groupList);
        result.put(ResultField.DATA, groupList);

        return result;
    }

    public void modifyGroup(String userName, Integer groupId, LinkedHashMap<String, Object> body) throws ItemDoNotExistsException, GroupAlreadyExistsException, JsonProcessingException, DataAccessException, UnknownColumnException, SQLException, ConditionNotAllowedException {
        Group group = groupRepository.getOne(groupId);
        if (group != null) {
            if (body.containsKey(COL.GROUP_NAME)) {
                String newGroupName = (String) body.get(COL.GROUP_NAME);

                if (groupRepository.existsByName(newGroupName))
                    throw new GroupAlreadyExistsException(newGroupName);

                group.setName(newGroupName);
            }

            if (body.containsKey(COL.DESCRIPTION)) {
                group.setDescription((String) body.get(COL.DESCRIPTION));
            }

            if (body.containsKey(COL.BUCKETS)) {
                group.setBuckets((List<Integer>) body.get(COL.BUCKETS));
            }

//            group.setUpdatedBy(userName);

            groupRepository.save(group);
        }
    }
}
