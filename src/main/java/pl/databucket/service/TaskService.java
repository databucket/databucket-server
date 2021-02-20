package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.dto.TaskDto;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.DataClass;
import pl.databucket.entity.Task;
import pl.databucket.exception.ItemNotFoundException;
import pl.databucket.exception.ModifyByNullEntityIdException;
import pl.databucket.repository.BucketRepository;
import pl.databucket.repository.DataClassRepository;
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

    public Task createTask(TaskDto taskDto) {
        Task task = new Task();
        task.setName(taskDto.getName());
        task.setDescription(taskDto.getDescription());
        task.setConfiguration(taskDto.getConfiguration());

        if (taskDto.getBuckets() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, taskDto.getBuckets());
            task.setBuckets(new HashSet<>(buckets));
        } else
            task.setBuckets(null);

        if (taskDto.getDataClasses() != null) {
            List<DataClass> dataClasses = dataClassRepository.findAllByDeletedAndIdIn(false, taskDto.getDataClasses());
            task.setDataClasses(new HashSet<>(dataClasses));
        }

        return taskRepository.save(task);
    }

    public List<Task> getTasks() {
        return taskRepository.findAllByDeletedOrderById(false);
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

        if (taskDto.getBuckets() != null) {
            List<Bucket> buckets = bucketRepository.findAllByDeletedAndIdIn(false, taskDto.getBuckets());
            task.setBuckets(new HashSet<>(buckets));
        } else
            task.setBuckets(null);

        if (taskDto.getDataClasses() != null) {
            List<DataClass> dataClasses = dataClassRepository.findAllByDeletedAndIdIn(false, taskDto.getDataClasses());
            task.setDataClasses(new HashSet<>(dataClasses));
        }

        return taskRepository.save(task);
    }

    public void deleteTask(long taskId) throws ItemNotFoundException {
        Task task = taskRepository.findByIdAndDeleted(taskId, false);

        if (task == null)
            throw new ItemNotFoundException(Task.class, taskId);

        task.setDeleted(true);
        taskRepository.save(task);
    }

}
