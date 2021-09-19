package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.DataClassDto;
import pl.databucket.server.entity.*;
import pl.databucket.server.exception.*;
import pl.databucket.server.repository.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class DataClassService {

    @Autowired
    private DataClassRepository dataClassRepository;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private DataColumnsRepository dataColumnsRepository;

    @Autowired
    private DataFilterRepository dataFilterRepository;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private TaskRepository taskRepository;


    public DataClass createDataClass(DataClassDto dataClassDto) throws ItemAlreadyExistsException {
        if (dataClassRepository.existsByNameAndDeleted(dataClassDto.getName(), false))
            throw new ItemAlreadyExistsException(DataClass.class, dataClassDto.getName());

        DataClass dataClass = new DataClass();
        dataClass.setName(dataClassDto.getName());
        dataClass.setDescription(dataClassDto.getDescription());
        if (dataClassDto.getConfiguration() != null && dataClassDto.getConfiguration().size() > 0)
            dataClass.setConfiguration(dataClassDto.getConfiguration());
        return dataClassRepository.save(dataClass);
    }

    public List<DataClass> getDataClasses() {
        return dataClassRepository.findAllByDeletedOrderById(false);
    }

    public DataClass modifyDataClass(DataClassDto dataClassDto) throws ItemNotFoundException, ItemAlreadyExistsException, ModifyByNullEntityIdException {
        if (dataClassDto.getId() == null)
            throw new ModifyByNullEntityIdException(DataClass.class);

        DataClass dataClass = dataClassRepository.findByIdAndDeleted(dataClassDto.getId(), false);

        if (dataClass == null)
            throw new ItemNotFoundException(DataClass.class, dataClassDto.getId());

        if (!dataClass.getName().equals(dataClassDto.getName()))
            if (dataClassRepository.existsByNameAndDeleted(dataClassDto.getName(), false))
                throw new ItemAlreadyExistsException(DataClass.class, dataClassDto.getName());

        dataClass.setName(dataClassDto.getName());
        dataClass.setDescription(dataClassDto.getDescription());

        if (dataClassDto.getConfiguration() != null && dataClassDto.getConfiguration().size() > 0)
            dataClass.setConfiguration(dataClassDto.getConfiguration());
        else
            dataClass.setConfiguration(null);
        return dataClassRepository.save(dataClass);
    }

    public void deleteDataClass(long dataClassId) throws ItemNotFoundException, ItemAlreadyUsedException {
        DataClass dataClass = dataClassRepository.findByIdAndDeleted(dataClassId, false);

        if (dataClass == null)
            throw new ItemNotFoundException(DataClass.class, dataClassId);

        Map<String, List<String>> usedByItems = new HashMap<>();
        List<Bucket> buckets = bucketRepository.findAllByDeletedAndDataClass(false, dataClass);
        if (buckets.size() > 0)
            usedByItems.put("buckets", buckets.stream().map(Bucket::getName).collect(Collectors.toList()));

        List<Tag> tags = tagRepository.findAllByDeletedOrderById(false)
                .stream()
                .filter(tag -> tag.getDataClasses().contains(dataClass))
                .collect(Collectors.toList());
        if (tags.size() > 0)
            usedByItems.put("tags", tags.stream().map(Tag::getName).collect(Collectors.toList()));

        List<DataColumns> columns = dataColumnsRepository.findAllByDeletedAndDataClass(false, dataClass);
        if (columns.size() > 0)
            usedByItems.put("columns", columns.stream().map(DataColumns::getName).collect(Collectors.toList()));

        List<DataFilter> filters = dataFilterRepository.findAllByDeletedAndDataClass(false, dataClass);
        if (filters.size() > 0)
            usedByItems.put("filters", filters.stream().map(DataFilter::getName).collect(Collectors.toList()));

        List<View> views = viewRepository.findAllByDeletedOrderById(false)
                .stream()
                .filter(view -> view.getDataClasses().contains(dataClass))
                .collect(Collectors.toList());
        if (views.size() > 0)
            usedByItems.put("views", views.stream().map(View::getName).collect(Collectors.toList()));

        List<Task> tasks = taskRepository.findAllByDeletedAndDataClass(false, dataClass);
        if (tasks.size() > 0)
            usedByItems.put("tasks", tasks.stream().map(Task::getName).collect(Collectors.toList()));

        if (usedByItems.size() > 0)
            throw new ItemAlreadyUsedException("Class", dataClass.getName(), usedByItems.toString());

        dataClass.setDeleted(true);
        dataClassRepository.save(dataClass);
    }

}
