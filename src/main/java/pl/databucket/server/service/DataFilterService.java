package pl.databucket.server.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.DataFilterDto;
import pl.databucket.server.entity.DataClass;
import pl.databucket.server.entity.DataFilter;
import pl.databucket.server.entity.Task;
import pl.databucket.server.entity.View;
import pl.databucket.server.exception.ItemAlreadyUsedException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.DataClassRepository;
import pl.databucket.server.repository.DataFilterRepository;
import pl.databucket.server.repository.TaskRepository;
import pl.databucket.server.repository.ViewRepository;


@Service
@RequiredArgsConstructor
public class DataFilterService {

    private final DataFilterRepository filterRepository;
    private final DataClassRepository dataClassRepository;
    private final ViewRepository viewRepository;
    private final TaskRepository taskRepository;


    public DataFilter createFilter(DataFilterDto dataFilterDto) throws ItemNotFoundException {
        DataFilter dataFilter = new DataFilter();
        dataFilter.setName(dataFilterDto.getName());
        dataFilter.setDescription(dataFilterDto.getDescription());
        dataFilter.setConfiguration(dataFilterDto.getConfiguration());

        if (dataFilterDto.getClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(dataFilterDto.getClassId(), false);
            if (dataClass != null) {
                dataFilter.setDataClass(dataClass);
            } else {
                throw new ItemNotFoundException(DataClass.class, dataFilterDto.getClassId());
            }
        }

        return filterRepository.save(dataFilter);
    }

    public List<DataFilter> getFilters() {
        return filterRepository.findAllByDeletedOrderById(false);
    }

    public List<DataFilter> getFilters(List<Long> ids) {
        return filterRepository.findAllByDeletedAndIdIn(false, ids);
    }

    public DataFilter modifyFilter(DataFilterDto dataFilterDto)
        throws ItemNotFoundException, ModifyByNullEntityIdException {
        if (dataFilterDto.getId() == null) {
            throw new ModifyByNullEntityIdException(DataFilter.class);
        }

        DataFilter dataFilter = filterRepository.findByIdAndDeleted(dataFilterDto.getId(), false);

        if (dataFilter == null) {
            throw new ItemNotFoundException(DataFilter.class, dataFilterDto.getId());
        }

        dataFilter.setName(dataFilterDto.getName());
        dataFilter.setDescription(dataFilterDto.getDescription());
        dataFilter.setConfiguration(dataFilterDto.getConfiguration());

        if (dataFilterDto.getClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(dataFilterDto.getClassId(), false);
            dataFilter.setDataClass(dataClass);
        } else {
            dataFilter.setDataClass(null);
        }

        return filterRepository.save(dataFilter);
    }

    public void deleteFilter(long filterId) throws ItemNotFoundException, ItemAlreadyUsedException {
        DataFilter dataFilter = filterRepository.findByIdAndDeleted(filterId, false);

        if (dataFilter == null) {
            throw new ItemNotFoundException(DataFilter.class, filterId);
        }

        Map<String, List<String>> usedByItems = new HashMap<>();

        List<Task> tasks = taskRepository.findAllByDeletedAndDataFilter(false, dataFilter);
        if (!tasks.isEmpty()) {
            usedByItems.put("tasks", tasks.stream().map(Task::getName).toList());
        }

        List<View> views = viewRepository.findAllByDeletedAndDataFilter(false, dataFilter);
        if (!views.isEmpty()) {
            usedByItems.put("views", views.stream().map(View::getName).toList());
        }

        if (usedByItems.size() > 0) {
            throw new ItemAlreadyUsedException("Filter", dataFilter.getName(), usedByItems.toString());
        }

        dataFilter.setDeleted(true);
        filterRepository.save(dataFilter);
    }
}
