package pl.databucket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.DataClass;

import java.util.List;

@Repository
public interface DataClassRepository extends JpaRepository<DataClass, Long> {

    DataClass findByIdAndDeleted(long id, boolean deleted);
    List<DataClass> findAllByDeletedAndIdIn(boolean deleted, Iterable<Long> ids);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    List<DataClass> findAllByDeletedOrderById(boolean deleted);
}
