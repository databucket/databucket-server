package pl.databucket.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.server.entity.DataClass;

import java.util.List;

@Repository
public interface DataClassRepository extends JpaRepository<DataClass, Long> {

    DataClass findByIdAndDeleted(long id, boolean deleted);
    DataClass findByNameAndDeleted(String name, boolean deleted);
    List<DataClass> findAllByDeletedAndIdIn(boolean deleted, Iterable<Long> ids);
    List<DataClass> findAllByDeletedAndNameIn(boolean deleted, Iterable<String> names);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    List<DataClass> findAllByDeletedOrderById(boolean deleted);
}
