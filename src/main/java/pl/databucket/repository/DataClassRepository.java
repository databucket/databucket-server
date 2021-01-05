package pl.databucket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.DataClass;

@Repository
public interface DataClassRepository extends JpaRepository<DataClass, Long> {

    boolean existsByNameAndDeleted(String name, boolean deleted);
    Page<DataClass> findAll(Specification<DataClass> specification, Pageable pageable);
}
