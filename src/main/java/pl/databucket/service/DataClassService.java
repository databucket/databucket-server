package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pl.databucket.dto.DataClassDto;
import pl.databucket.entity.DataClass;
import pl.databucket.exception.*;
import pl.databucket.repository.DataClassRepository;


@Service
public class DataClassService {

    @Autowired
    private DataClassRepository dataClassRepository;

    public DataClass createDataClass(DataClassDto dataClassDto) {
        DataClass dataClass = new DataClass();
        dataClass.setName(dataClassDto.getName());
        dataClass.setDescription(dataClass.getDescription());

        return dataClassRepository.save(dataClass);
    }

    public Page<DataClass> getDataClasses(Specification<DataClass> specification, Pageable pageable) {
        return dataClassRepository.findAll(specification, pageable);
    }

    public void deleteDataClass(long classId) {
        DataClass dataClass = dataClassRepository.getOne(classId);
        dataClass.setDeleted(true);
        dataClassRepository.save(dataClass);
    }

//    private String referencesClassItem(int classId) throws UnknownColumnException, ConditionNotAllowedException {
//        final String AS_ID = " as \"id\"";
//        final String AS_NAME = " as \"name\"";
//        String result = "";
//
//        List<Condition> conditions = new ArrayList<>();
//        conditions.add(new Condition(COL.CLASS_ID, Operator.equal, classId));
//        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
//
//        Map<String, Object> paramMap = new HashMap<>();
//
//        Query query = new Query(TAB.BUCKET, true)
//                .select(new String[]{COL.BUCKET_ID + AS_ID, COL.BUCKET_NAME + AS_NAME})
//                .from()
//                .where(conditions, paramMap);
//        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
//        result += serviceUtils.getStringWithItemsNames(C.BUCKETS, resultList);
//
//        query = new Query(TAB.TAG, true)
//                .select(new String[]{COL.TAG_ID + AS_ID, COL.TAG_NAME + AS_NAME})
//                .from()
//                .where(conditions, paramMap);
//        resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
//        result += serviceUtils.getStringWithItemsNames(C.TAGS, resultList);
//
//        query = new Query(TAB.COLUMNS, true)
//                .select(new String[]{COL.COLUMNS_ID + AS_ID, COL.COLUMNS_NAME + AS_NAME})
//                .from()
//                .where(conditions, paramMap);
//        resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
//        result += serviceUtils.getStringWithItemsNames(C.COLUMNS, resultList);
//
//        query = new Query(TAB.FILTER, true)
//                .select(new String[]{COL.FILTER_ID + AS_ID, COL.FILTER_NAME + AS_NAME})
//                .from()
//                .where(conditions, paramMap);
//        resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
//        result += serviceUtils.getStringWithItemsNames(C.FILTERS, resultList);
//
//        query = new Query(TAB.TASK, true)
//                .select(new String[]{COL.TASK_ID + AS_ID, COL.TASK_NAME + AS_NAME})
//                .from()
//                .where(conditions, paramMap);
//        resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
//        result += serviceUtils.getStringWithItemsNames(C.TASKS, resultList);
//
//        query = new Query(TAB.VIEW, true)
//                .select(new String[]{COL.VIEW_ID + AS_ID, COL.VIEW_NAME + AS_NAME})
//                .from()
//                .where(conditions, paramMap);
//        resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
//        result += serviceUtils.getStringWithItemsNames(C.VIEWS, resultList);
//
//        query = new Query(TAB.EVENT, true)
//                .select(new String[]{COL.EVENT_ID + AS_ID, COL.EVENT_NAME + AS_NAME})
//                .from()
//                .where(conditions, paramMap);
//        resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
//        result += serviceUtils.getStringWithItemsNames(C.EVENTS, resultList);
//
//        if (result.length() > 0)
//            return result;
//        else
//            return null;
//    }

    public DataClass modifyDataClass(DataClassDto dataClassDto) {
        DataClass dataClass = dataClassRepository.getOne(dataClassDto.getId());
        dataClass.setName(dataClassDto.getName());
        dataClass.setDescription(dataClassDto.getDescription());
        return dataClassRepository.save(dataClass);
    }



}
