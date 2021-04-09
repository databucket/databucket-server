package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.dto.TaskDto;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.DataClass;
import pl.databucket.entity.DataFilter;
import pl.databucket.entity.Task;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.repository.BucketRepository;
import pl.databucket.repository.DataClassRepository;
import pl.databucket.repository.DataFilterRepository;
import pl.databucket.repository.TaskRepository;

import java.util.HashSet;
import java.util.List;


@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private BucketRepository bucketRepository;

    @Autowired
    private DataClassRepository dataClassRepository;

    @Autowired
    private DataFilterRepository dataFilterRepository;


    public Task createTask(TaskDto taskDto) throws ItemNotFoundException {
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

        return taskRepository.save(task);
    }

    public List<Task> getTasks() {
        return taskRepository.findAllByDeletedOrderById(false);
    }

    public List<Task> getTasks(List<Long> ids) {
        return taskRepository.findAllByDeletedAndIdIn(false, ids);
    }

    public Task modifyTask(TaskDto taskDto) throws ItemNotFoundException, ModifyByNullEntityIdException {
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

        return taskRepository.save(task);
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
