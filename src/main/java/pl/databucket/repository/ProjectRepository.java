package pl.databucket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Project;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    Project findByIdAndDeleted(int id, boolean deleted);
    List<Project> findAllByDeletedAndIdIn(boolean deleted, Iterable<Integer> ids);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    Page<Project> findAll(Specification<Project> specification, Pageable pageable);
}
