package pl.databucket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Columns;

@Repository
public interface ColumnsRepository extends JpaRepository<Columns, Long> {

    boolean existsByNameAndDeleted(String name, boolean deleted);
    Columns findColumnsByName(String name);
    Page<Columns> findAll(Specification<Columns> specification, Pageable pageable);
}
