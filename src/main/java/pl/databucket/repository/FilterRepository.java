package pl.databucket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Filter;

@Repository
public interface FilterRepository extends JpaRepository<Filter, Long> {

    boolean existsByNameAndDeleted(String name, boolean deleted);
    Page<Filter> findAll(Specification<Filter> specification, Pageable pageable);
}
