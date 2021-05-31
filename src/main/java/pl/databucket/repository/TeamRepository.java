package pl.databucket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Team;

import java.util.List;

@Repository
public interface TeamRepository extends JpaRepository<Team, Short> {

    Team findByIdAndDeleted(short id, boolean deleted);
    List<Team> findAllByIdIn(Iterable<Short> ids);
    List<Team> findAllByDeletedAndIdIn(boolean deleted, Iterable<Short> ids);
    List<Team> findAllByDeletedOrderById(boolean deleted);
    boolean existsByNameAndDeleted(String name, boolean deleted);
}
