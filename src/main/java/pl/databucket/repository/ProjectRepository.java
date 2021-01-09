package pl.databucket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Filter;
import pl.databucket.entity.Project;
import pl.databucket.entity.Role;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {
    Project findByName(String name);
    boolean existsByName(String name);
    Page<Project> findAll(Specification<Project> specification, Pageable pageable);
}
