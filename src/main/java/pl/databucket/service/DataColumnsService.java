package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.DataClass;
import pl.databucket.exception.*;
import pl.databucket.dto.DataColumnsDto;
import pl.databucket.entity.DataColumns;
import pl.databucket.repository.BucketRepository;
import pl.databucket.repository.DataColumnsRepository;
import pl.databucket.repository.DataClassRepository;


@Service
public class DataColumnsService {

    @Autowired
    private DataColumnsRepository columnsRepository;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    public DataColumns createColumns(DataColumnsDto dataColumnsDto) throws ItemNotFoundException {
        DataColumns dataColumns = new DataColumns();
        dataColumns.setName(dataColumnsDto.getName());
        dataColumns.setDescription(dataColumnsDto.getDescription());

        if (dataColumnsDto.getBucketId() != null) {
            Bucket bucket = bucketRepository.findByIdAndDeleted(dataColumnsDto.getBucketId(), false);
            if (bucket != null)
                dataColumns.setBucket(bucket);
            else
                throw new ItemNotFoundException(Bucket.class, dataColumnsDto.getBucketId());
        }

        if (dataColumnsDto.getDataClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(dataColumnsDto.getDataClassId(), false);
            if (dataClass != null)
                dataColumns.setDataClass(dataClass);
            else
                throw new ItemNotFoundException(DataClass.class, dataColumnsDto.getDataClassId());
        }

        dataColumns.setColumns(dataColumnsDto.getColumns());

        return columnsRepository.save(dataColumns);
    }

    public Page<DataColumns> getColumns(Specification<DataColumns> specification, Pageable pageable) {
        return columnsRepository.findAll(specification, pageable);
    }

    public DataColumns modifyColumns(DataColumnsDto dataColumnsDto) throws ItemNotFoundException, ModifyByNullEntityIdException {
        if (dataColumnsDto.getId() == null)
            throw new ModifyByNullEntityIdException(DataColumns.class);

        DataColumns dataColumns = columnsRepository.findByIdAndDeleted(dataColumnsDto.getId(), false);

        if (dataColumns == null)
            throw new ItemNotFoundException(DataColumns.class, dataColumnsDto.getId());

        dataColumns.setName(dataColumnsDto.getName());
        dataColumns.setDescription(dataColumnsDto.getDescription());

        if (dataColumnsDto.getBucketId() != null) {
            Bucket bucket = bucketRepository.findByIdAndDeleted(dataColumnsDto.getBucketId(), false);
            if (bucket != null)
                dataColumns.setBucket(bucket);
            else
                throw new ItemNotFoundException(Bucket.class, dataColumnsDto.getBucketId());
        } else
            dataColumns.setBucket(null);

        if (dataColumnsDto.getDataClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(dataColumnsDto.getDataClassId(), false);
            if (dataClass != null)
                dataColumns.setDataClass(dataClass);
            else
                throw new ItemNotFoundException(DataClass.class, dataColumnsDto.getDataClassId());
        } else
            dataColumns.setDataClass(null);

        dataColumns.setColumns(dataColumnsDto.getColumns());

        return columnsRepository.save(dataColumns);
    }

    public void deleteColumns(long columnsId) throws ItemNotFoundException {
        DataColumns columns = columnsRepository.findByIdAndDeleted(columnsId, false);

        if (columns == null)
            throw new ItemNotFoundException(DataColumns.class, columnsId);

        columns.setDeleted(true);
        columnsRepository.save(columns);
    }

}
