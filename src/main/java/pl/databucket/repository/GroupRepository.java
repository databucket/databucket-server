package pl.databucket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    boolean existsByNameAndDeleted(String name, boolean deleted);
    Page<Group> findAll(Specification<Group> specification, Pageable pageable);
}