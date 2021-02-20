package pl.databucket.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.EventLog;

@Repository
public interface EventLogRepository extends JpaRepository<EventLog, Long> {
    Page<EventLog> findAll(Specification<EventLog> specification, Pageable pageable);
}
