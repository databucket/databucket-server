package pl.databucket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.View;

@Repository
public interface ViewRepository extends JpaRepository<View, Long> {
    boolean existsByNameAndDeleted(String name, boolean deleted);
    Page<View> findAll(Specification<View> specification, Pageable pageable);
}
