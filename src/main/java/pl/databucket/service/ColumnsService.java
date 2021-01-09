package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pl.databucket.exception.*;
import pl.databucket.dto.DataColumnsDto;
import pl.databucket.entity.DataColumns;
import pl.databucket.repository.BucketRepository;
import pl.databucket.repository.DataColumnsRepository;
import pl.databucket.repository.DataClassRepository;


@Service
public class ColumnsService {

    @Autowired
    private DataColumnsRepository columnsRepository;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    public DataColumns createColumns(DataColumnsDto columnsDto) {
        DataColumns columns = new DataColumns();
        columns.setName(columnsDto.getName());
        columns.setDescription(columnsDto.getDescription());

        if (columnsDto.getBucketId() != null)
            columns.setBucket(bucketRepository.getOne(columnsDto.getBucketId()));

        if (columnsDto.getDataClassId() != null)
            columns.setDataClass(dataClassRepository.getOne(columnsDto.getDataClassId()));

        columns.setColumns(columnsDto.getColumns());

        return columnsRepository.save(columns);
    }

    public Page<DataColumns> getColumns(Specification<DataColumns> specification, Pageable pageable) {
        return columnsRepository.findAll(specification, pageable);
    }

    public DataColumns modifyColumns(DataColumnsDto columnsDto) {
        DataColumns columns = columnsRepository.getOne(columnsDto.getId());

        columns.setName(columnsDto.getName());
        columns.setDescription(columnsDto.getDescription());

        if (columns.getBucket() != null) {
            if (columnsDto.getBucketId() == null)
                columns.setBucket(null);
            else if (columns.getBucket().getId() != columnsDto.getBucketId())
                columns.setBucket(bucketRepository.getOne(columnsDto.getBucketId()));
        } else if (columnsDto.getBucketId() != null)
            columns.setBucket(bucketRepository.getOne(columnsDto.getBucketId()));

        if (columns.getDataClass() != null) {
            if (columnsDto.getDataClassId() == null)
                columns.setDataClass(null);
            else if (columns.getDataClass().getId() != columnsDto.getDataClassId())
                columns.setDataClass(dataClassRepository.getOne(columnsDto.getDataClassId()));
        } else if (columnsDto.getDataClassId() != null)
            columns.setDataClass(dataClassRepository.getOne(columnsDto.getDataClassId()));

        columns.setColumns(columnsDto.getColumns());

        return columnsRepository.save(columns);
    }

    public void deleteColumns(long columnsId) throws ItemNotFoundException, ItemAlreadyUsedException, UnknownColumnException, ConditionNotAllowedException {
        DataColumns columns = columnsRepository.getOne(columnsId);
        columns.setDeleted(true);
        columnsRepository.save(columns);
    }

//    private String referencesColumnsItem(int columnsId) throws UnknownColumnException, ConditionNotAllowedException {
//        final String AS_ID = " as \"id\"";
//        final String AS_NAME = " as \"name\"";
//        String result = "";
//
//        List<Condition> conditions = new ArrayList<>();
//        conditions.add(new Condition(COL.COLUMNS_ID, Operator.equal, columnsId));
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
