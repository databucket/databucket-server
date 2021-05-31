package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.dto.DataFilterDto;
import pl.databucket.entity.DataClass;
import pl.databucket.entity.DataFilter;
import pl.databucket.entity.Task;
import pl.databucket.entity.View;
import pl.databucket.exception.ItemAlreadyUsedException;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.repository.DataClassRepository;
import pl.databucket.repository.DataFilterRepository;
import pl.databucket.repository.TaskRepository;
import pl.databucket.repository.ViewRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class DataFilterService {

    @Autowired
    private DataFilterRepository filterRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private TaskRepository taskRepository;


    public DataFilter createFilter(DataFilterDto dataFilterDto) throws ItemNotFoundException {
        DataFilter dataFilter = new DataFilter();
        dataFilter.setName(dataFilterDto.getName());
        dataFilter.setDescription(dataFilterDto.getDescription());
        dataFilter.setConfiguration(dataFilterDto.getConfiguration());

        if (dataFilterDto.getClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(dataFilterDto.getClassId(), false);
            if (dataClass != null)
                dataFilter.setDataClass(dataClass);
            else
                throw new ItemNotFoundException(DataClass.class, dataFilterDto.getClassId());
        }

        return filterRepository.save(dataFilter);
    }

    public List<DataFilter> getFilters() {
        return filterRepository.findAllByDeletedOrderById(false);
    }

    public List<DataFilter> getFilters(List<Long> ids) {
        return filterRepository.findAllByDeletedAndIdIn(false, ids);
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

        if (dataFilterDto.getClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(dataFilterDto.getClassId(), false);
            dataFilter.setDataClass(dataClass);
        } else
            dataFilter.setDataClass(null);

        return filterRepository.save(dataFilter);
    }

    public void deleteFilter(long filterId) throws ItemNotFoundException, ItemAlreadyUsedException {
        DataFilter dataFilter = filterRepository.findByIdAndDeleted(filterId, false);

        if (dataFilter == null)
            throw new ItemNotFoundException(DataFilter.class, filterId);

        Map<String, List<String>> usedByItems = new HashMap<>();

        List<Task> tasks = taskRepository.findAllByDeletedAndDataFilter(false, dataFilter);
        if (tasks.size() > 0)
            usedByItems.put("tasks", tasks.stream().map(Task::getName).collect(Collectors.toList()));

        List<View> views = viewRepository.findAllByDeletedAndDataFilter(false, dataFilter);
        if (views.size() > 0)
            usedByItems.put("views", views.stream().map(View::getName).collect(Collectors.toList()));

        if (usedByItems.size() > 0)
            throw new ItemAlreadyUsedException("Filter", dataFilter.getName(), usedByItems.toString());

        dataFilter.setDeleted(true);
        filterRepository.save(dataFilter);
    }
}
