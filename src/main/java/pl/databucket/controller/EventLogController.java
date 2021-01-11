package pl.databucket.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.service.EventLogService;
import pl.databucket.specification.EventLogSpecification;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/events/log")
@RestController
public class EventLogController {

  private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(EventLogController.class);

  @Autowired
  private EventLogService eventLogService;

  @Autowired
  private ModelMapper modelMapper;

  @GetMapping
  public ResponseEntity<?> getEventLog(EventLogSpecification specification, Pageable pageable) {
    try {
      return new ResponseEntity<>(eventLogService.getEventLog(specification, pageable), HttpStatus.OK);
    } catch (Exception ee) {
      return exceptionFormatter.defaultException(ee);
    }
  }

  @DeleteMapping
  public ResponseEntity<?> clearEventLog() {
    try {
      eventLogService.clearEventsLog();
      return new ResponseEntity<>(null, HttpStatus.OK);
    } catch (Exception e) {
      return exceptionFormatter.defaultException(e);
    }
  }
}
