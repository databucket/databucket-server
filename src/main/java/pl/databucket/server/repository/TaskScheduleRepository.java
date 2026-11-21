package pl.databucket.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.server.entity.Task;
import pl.databucket.server.entity.TaskSchedule;

import java.util.List;

@Repository
public interface TaskScheduleRepository extends JpaRepository<TaskSchedule, Long> {

    TaskSchedule findByIdAndDeleted(long id, boolean deleted);

    List<TaskSchedule> findAllByDeletedOrderById(boolean deleted);

    List<TaskSchedule> findAllByDeletedAndTask(boolean deleted, Task task);

    List<TaskSchedule> findAllByDeletedAndBucket(boolean deleted, pl.databucket.server.entity.Bucket bucket);
}
