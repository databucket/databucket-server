package pl.databucket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    boolean existsByNameAndDeleted(String name, boolean deleted);
    Page<Event> findAll(Specification<Event> specification, Pageable pageable);
}
