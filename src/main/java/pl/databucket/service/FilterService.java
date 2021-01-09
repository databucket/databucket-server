package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pl.databucket.dto.DataFilterDto;
import pl.databucket.entity.DataFilter;
import pl.databucket.repository.DataFilterRepository;


@Service
public class FilterService {

    @Autowired
    private DataFilterRepository filterRepository;

    public DataFilter createFilter(DataFilterDto filterDto) {
        DataFilter filter = new DataFilter();
        filter.setName(filterDto.getName());
        filter.setDescription(filterDto.getDescription());
        filter.setCriteria(filterDto.getCreteria());
//        filter.setDataClasses(); TODO
//        filter.setBuckets(); TODO
        return filterRepository.save(filter);
    }

    public Page<DataFilter> getFilters(Specification<DataFilter> specification, Pageable pageable) {
        return filterRepository.findAll(specification, pageable);
    }

    public DataFilter modifyFilter(DataFilterDto filterDto) {
        DataFilter filter = filterRepository.getOne(filterDto.getId());
        filter.setName(filterDto.getName());
        filter.setDescription(filterDto.getDescription());
        filter.setCriteria(filterDto.getCreteria());
//        filter.setDataClasses(); TODO
//        filter.setBuckets(); TODO
        return filterRepository.save(filter);
    }

    public void deleteFilter(long filterId) {
        DataFilter filter = filterRepository.getOne(filterId);
        filter.setDeleted(true);
        filterRepository.save(filter);
    }

//    private String referencesFilterItem(int filterId) throws UnknownColumnException, ConditionNotAllowedException {
//        final String AS_ID = " as \"id\"";
//        final String AS_NAME = " as \"name\"";
//        String result = "";
//
//        List<Condition> conditions = new ArrayList<>();
//        conditions.add(new Condition(COL.FILTER_ID, Operator.equal, filterId));
//        conditions.add(new Condition(COL.DELETED, Operator.equal, false));
//
//        Map<String, Object> paramMap = new HashMap<>();
//
//        Query query = new Query(TAB.VIEW, true)
//                .select(new String[]{COL.VIEW_ID + AS_ID, COL.VIEW_NAME + AS_NAME})
//                .from()
//                .where(conditions, paramMap);
//
//        List<Map<String, Object>> resultList = jdbcTemplate.queryForList(query.toString(logger, paramMap), paramMap);
//        result += serviceUtils.getStringWithItemsNames(C.VIEWS, resultList);
//
//        if (result.length() > 0)
//            return result;
//        else
//            return null;
//    }


}
