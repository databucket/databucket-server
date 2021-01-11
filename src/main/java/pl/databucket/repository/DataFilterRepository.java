package pl.databucket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.DataFilter;

@Repository
public interface DataFilterRepository extends JpaRepository<DataFilter, Long> {

    DataFilter findByIdAndDeleted(long id, boolean deleted);
    boolean existsByNameAndDeleted(String name, boolean deleted);
    Page<DataFilter> findAll(Specification<DataFilter> specification, Pageable pageable);
}
