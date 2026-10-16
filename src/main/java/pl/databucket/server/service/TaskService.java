package pl.databucket.server.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.databucket.server.dto.TaskDto;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.DataClass;
import pl.databucket.server.entity.DataFilter;
import pl.databucket.server.entity.Task;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.BucketRepository;
import pl.databucket.server.repository.DataClassRepository;
import pl.databucket.server.repository.DataFilterRepository;
import pl.databucket.server.repository.TaskRepository;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional
public class TaskService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    @Autowired
    private DataFilterRepository dataFilterRepository;


    public TaskDto createTask(TaskDto taskDto) throws ItemNotFoundException {
        Task task = new Task();
        task.setName(taskDto.getName());
        task.setDescription(taskDto.getDescription());
        task.setConfiguration(taskDto.getConfiguration());

        if (taskDto.getClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(taskDto.getClassId(), false);
            task.setDataClass(dataClass);
        }

        if (taskDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, taskDto.getBucketsIds());
            task.setBuckets(new HashSet<>(buckets));
        }

        if (taskDto.getClassesIds() != null) {
            List<DataClass> dataClasses = dataClassRepository.findAllByDeletedAndIdIn(false, taskDto.getClassesIds());
            task.setDataClasses(new HashSet<>(dataClasses));
        }

        if (taskDto.getFilterId() != null) {
            DataFilter dataFilter = dataFilterRepository.findByIdAndDeleted(taskDto.getFilterId(), false);
            task.setDataFilter(dataFilter);
        }

        modelMapper.map(taskRepository.save(task), taskDto);
        return taskDto;
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasks() {
        return taskRepository.findAllByDeletedOrderById(false).stream()
                .map(item -> modelMapper.map(item, TaskDto.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasks(List<Long> ids) {
        return taskRepository.findAllByDeletedAndIdIn(false, ids).stream()
                .map(item -> modelMapper.map(item, TaskDto.class))
                .collect(Collectors.toList());
    }

    public TaskDto modifyTask(TaskDto taskDto) throws ItemNotFoundException, ModifyByNullEntityIdException {
        if (taskDto.getId() == null)
            throw new ModifyByNullEntityIdException(Task.class);

        Task task = taskRepository.findByIdAndDeleted(taskDto.getId(), false);

        if (task == null)
            throw new ItemNotFoundException(Task.class, taskDto.getId());

        task.setName(taskDto.getName());
        task.setDescription(taskDto.getDescription());
        task.setConfiguration(taskDto.getConfiguration());

        if (taskDto.getClassId() != null) {
            DataClass dataClass = dataClassRepository.findByIdAndDeleted(taskDto.getClassId(), false);
            task.setDataClass(dataClass);
        } else
            task.setDataClass(null);

        if (taskDto.getBucketsIds() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, taskDto.getBucketsIds());
            task.setBuckets(new HashSet<>(buckets));
        } else
            task.setBuckets(null);

        if (taskDto.getClassesIds() != null) {
            List<DataClass> dataClasses = dataClassRepository.findAllByDeletedAndIdIn(false, taskDto.getClassesIds());
            task.setDataClasses(new HashSet<>(dataClasses));
        } else
            task.setDataClasses(null);

        if (taskDto.getFilterId() != null) {
            DataFilter dataFilter = dataFilterRepository.findByIdAndDeleted(taskDto.getFilterId(), false);
            task.setDataFilter(dataFilter);
        } else
            task.setDataFilter(null);

        modelMapper.map(taskRepository.save(task), taskDto);
        return taskDto;
    }

    public void deleteTask(long taskId) throws ItemNotFoundException {
        Task task = taskRepository.findByIdAndDeleted(taskId, false);

        if (task == null)
            throw new ItemNotFoundException(Task.class, taskId);

        task.setBuckets(null);
        task.setDataClasses(null);

        task.setDeleted(true);
        taskRepository.save(task);
    }

}
