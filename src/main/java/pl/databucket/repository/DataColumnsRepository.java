package pl.databucket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.DataColumns;

import java.util.List;

@Repository
public interface DataColumnsRepository extends JpaRepository<DataColumns, Long> {

    DataColumns findByIdAndDeleted(long id, boolean deleted);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    List<DataColumns> findAllByDeletedOrderById(boolean deleted);
    List<DataColumns> findAllByDeletedAndIdIn(boolean deleted, Iterable<Long> ids);
}
