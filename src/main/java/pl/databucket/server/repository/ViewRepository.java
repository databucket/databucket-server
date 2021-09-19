package pl.databucket.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.server.entity.DataColumns;
import pl.databucket.server.entity.DataFilter;
import pl.databucket.server.entity.View;

import java.util.List;

@Repository
public interface ViewRepository extends JpaRepository<View, Long> {

    View findByIdAndDeleted(long id, boolean deleted);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    List<View> findAllByDeletedAndIdIn(boolean deleted, Iterable<Long> ids);
    List<View> findAllByDeletedAndDataColumns(boolean deleted, DataColumns dataColumns);
    List<View> findAllByDeletedAndDataFilter(boolean deleted, DataFilter dataFilter);
    List<View> findAllByDeletedOrderById(boolean deleted);
}
