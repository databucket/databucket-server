package pl.databucket.server.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.server.entity.TaskScheduleRun;

import java.util.List;

@Repository
public interface TaskScheduleRunRepository extends JpaRepository<TaskScheduleRun, Long> {

    List<TaskScheduleRun> findAllByScheduleIdOrderByIdDesc(long scheduleId, Pageable pageable);
}
