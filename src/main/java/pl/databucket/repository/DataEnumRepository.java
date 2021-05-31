package pl.databucket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.DataEnum;

import java.util.List;

@Repository
public interface DataEnumRepository extends JpaRepository<DataEnum, Integer> {

    DataEnum findByIdAndDeleted(int id, boolean deleted);
    List<DataEnum> findAllByDeletedAndIdIn(boolean deleted, Iterable<Integer> ids);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    List<DataEnum> findAllByDeletedOrderById(boolean deleted);
}
