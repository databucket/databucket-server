package pl.databucket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.DataFilter;

import java.util.List;

@Repository
public interface DataFilterRepository extends JpaRepository<DataFilter, Long> {

    DataFilter findByIdAndDeleted(long id, boolean deleted);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    List<DataFilter> findAllByDeletedOrderById(boolean deleted);
    List<DataFilter> findAllByDeletedAndIdIn(boolean deleted, Iterable<Long> ids);
}
