package pl.databucket.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.server.entity.Project;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    Project findByIdAndDeleted(int id, boolean deleted);
    List<Project> findAllByDeletedAndIdIn(boolean deleted, Iterable<Integer> ids);
    List<Project> findAllByDeletedOrderById(boolean deleted);
    boolean existsByNameAndDeleted(String name, boolean deleted);
}
