package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pl.databucket.dto.DataFilterDto;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.DataClass;
import pl.databucket.entity.DataFilter;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.repository.BucketRepository;
import pl.databucket.repository.DataClassRepository;
import pl.databucket.repository.DataFilterRepository;

import java.util.HashSet;
import java.util.List;


@Service
public class DataFilterService {

    @Autowired
    private DataFilterRepository filterRepository;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    public DataFilter createFilter(DataFilterDto dataFilterDto) {
        DataFilter dataFilter = new DataFilter();
        dataFilter.setName(dataFilterDto.getName());
        dataFilter.setDescription(dataFilterDto.getDescription());
        dataFilter.setCriteria(dataFilterDto.getCreteria());

        if (dataFilterDto.getBuckets() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, dataFilterDto.getBuckets());
            dataFilter.setBuckets(new HashSet<>(buckets));
        } else
            dataFilter.setBuckets(null);

        if (dataFilterDto.getDataClasses() != null) {
            List<DataClass> dataClasses = dataClassRepository.findAllByDeletedAndIdIn(false, dataFilterDto.getDataClasses());
            dataFilter.setDataClasses(new HashSet<>(dataClasses));
        } else
            dataFilter.setDataClasses(null);

        return filterRepository.save(dataFilter);
    }

    public Page<DataFilter> getFilters(Specification<DataFilter> specification, Pageable pageable) {
        return filterRepository.findAll(specification, pageable);
    }

    public DataFilter modifyFilter(DataFilterDto dataFilterDto) throws ItemNotFoundException, ModifyByNullEntityIdException {
        if (dataFilterDto.getId() == null)
            throw new ModifyByNullEntityIdException(DataFilter.class);

        DataFilter dataFilter = filterRepository.findByIdAndDeleted(dataFilterDto.getId(), false);

        if (dataFilter == null)
            throw new ItemNotFoundException(DataFilter.class, dataFilterDto.getId());

        dataFilter.setName(dataFilterDto.getName());
        dataFilter.setDescription(dataFilterDto.getDescription());
        dataFilter.setCriteria(dataFilterDto.getCreteria());

        if (dataFilterDto.getBuckets() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, dataFilterDto.getBuckets());;
            dataFilter.setBuckets(new HashSet<>(buckets));
        } else
            dataFilter.setBuckets(null);

        if (dataFilterDto.getDataClasses() != null) {
            List<DataClass> dataClasses = dataClassRepository.findAllByDeletedAndIdIn(false, dataFilterDto.getDataClasses());
            dataFilter.setDataClasses(new HashSet<>(dataClasses));
        } else
            dataFilter.setDataClasses(null);

        return filterRepository.save(dataFilter);
    }

    public void deleteFilter(long filterId) throws ItemNotFoundException {
        DataFilter dataFilter = filterRepository.findByIdAndDeleted(filterId, false);

        if (dataFilter == null)
            throw new ItemNotFoundException(DataFilter.class, filterId);

        dataFilter.setDeleted(true);
        filterRepository.save(dataFilter);
    }
}
