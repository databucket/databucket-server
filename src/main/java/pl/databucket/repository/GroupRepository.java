package pl.databucket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Group;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    Group findByIdAndDeleted(long id, boolean deleted);
    List<Group> findAllByDeletedAndIdIn(boolean deleted, Iterable<Long> ids);
    List<Group> findAllByDeletedOrderById(boolean deleted);
    boolean existsByNameAndDeleted(String name, boolean deleted);
}
