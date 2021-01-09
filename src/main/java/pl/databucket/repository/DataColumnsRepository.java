package pl.databucket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.DataColumns;

@Repository
public interface DataColumnsRepository extends JpaRepository<DataColumns, Long> {

    boolean existsByNameAndDeleted(String name, boolean deleted);
    DataColumns findColumnsByName(String name);
    Page<DataColumns> findAll(Specification<DataColumns> specification, Pageable pageable);
}
