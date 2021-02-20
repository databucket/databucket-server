package pl.databucket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Task;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    Task findByIdAndDeleted(long id, boolean deleted);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    List<Task> findAllByDeletedOrderById(boolean deleted);
}
