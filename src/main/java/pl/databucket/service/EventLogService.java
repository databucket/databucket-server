package pl.databucket.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import pl.databucket.entity.EventLog;
import pl.databucket.repository.EventLogRepository;


@Service
public class EventLogService {

    @Autowired
    private EventLogRepository eventLogRepository;

    public Page<EventLog> getEventLog(Specification<EventLog> specification, Pageable pageable) {
        return eventLogRepository.findAll(specification, pageable);
    }

    public void clearEventsLog() {
        eventLogRepository.deleteAll();
    }

}
