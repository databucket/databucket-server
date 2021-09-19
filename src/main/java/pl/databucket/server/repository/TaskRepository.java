package pl.databucket.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.server.entity.DataClass;
import pl.databucket.server.entity.DataFilter;
import pl.databucket.server.entity.Task;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Task findByIdAndDeleted(long id, boolean deleted);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    List<Task> findAllByDeletedOrderById(boolean deleted);
    List<Task> findAllByDeletedAndDataClass(boolean deleted, DataClass dataClass);
    List<Task> findAllByDeletedAndDataFilter(boolean deleted, DataFilter dataFilter);
    List<Task> findAllByDeletedAndIdIn(boolean deleted, Iterable<Long> ids);
}
