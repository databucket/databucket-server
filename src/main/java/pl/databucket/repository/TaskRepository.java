package pl.databucket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    boolean existsByNameAndDeleted(String name, boolean deleted);
    Page<Task> findAll(Specification<Task> specification, Pageable pageable);
}
