package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.dto.DataFilterDto;
import pl.databucket.entity.DataFilter;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.repository.DataFilterRepository;

import java.util.List;


@Service
public class DataFilterService {

    @Autowired
    private DataFilterRepository filterRepository;

    public DataFilter createFilter(DataFilterDto dataFilterDto) {
        DataFilter dataFilter = new DataFilter();
        dataFilter.setName(dataFilterDto.getName());
        dataFilter.setDescription(dataFilterDto.getDescription());
        dataFilter.setConfiguration(dataFilterDto.getConfiguration());

        return filterRepository.save(dataFilter);
    }

    public List<DataFilter> getFilters() {
        return filterRepository.findAllByDeletedOrderById(false);
    }

    public DataFilter modifyFilter(DataFilterDto dataFilterDto) throws ItemNotFoundException, ModifyByNullEntityIdException {
        if (dataFilterDto.getId() == null)
            throw new ModifyByNullEntityIdException(DataFilter.class);

        DataFilter dataFilter = filterRepository.findByIdAndDeleted(dataFilterDto.getId(), false);

        if (dataFilter == null)
            throw new ItemNotFoundException(DataFilter.class, dataFilterDto.getId());

        dataFilter.setName(dataFilterDto.getName());
        dataFilter.setDescription(dataFilterDto.getDescription());
        dataFilter.setConfiguration(dataFilterDto.getConfiguration());

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
