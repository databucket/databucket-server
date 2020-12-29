package pl.databucket.controller;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.database.ResultField;
import pl.databucket.exception.CustomExceptionFormatter;
import pl.databucket.service.EventService;
import pl.databucket.response.BaseResponse;
import pl.databucket.response.EventResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/event/log")
@RestController
public class EventLogController {

  private final CustomExceptionFormatter customExceptionFormatter;
  private final EventService service;

  public EventLogController(EventService service) {
    this.service = service;
    this.customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(EventLogController.class));
  }
  
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> getEventsLog(
      @RequestParam(required = false) Optional<Integer> page,
      @RequestParam(required = false) Optional<Integer> limit,
      @RequestParam(required = false) Optional<String> sort,
      @RequestParam(required = false) Optional<String> filter) {

    EventResponse response = new EventResponse();
    try {
      if (page.isPresent()) {
        FieldValidator.mustBeGraterThen0("page", page.get());
        response.setPage(page.get());
      }

      if (limit.isPresent()) {
        FieldValidator.mustBeGraterOrEqual0("limit", limit.get());
        response.setLimit(limit.get());
      }

      if (sort.isPresent()) {
        FieldValidator.validateSort(sort.get());
        response.setSort(sort.get());
      }

      List<Condition> urlConditions = null;
      if (filter.isPresent()) {
        urlConditions = FieldValidator.validateFilter(filter.get());
      }

      Map<ResultField, Object> result = service.getEventsLog(page, limit, sort, urlConditions);

      long total = (long) result.get(ResultField.TOTAL);
      response.setTotal(total);

      if (page.isPresent() && limit.isPresent()) {
        response.setTotalPages((int) Math.ceil(total / (float) limit.get()));
      }

      response.setEventsLog((List<Map<String, Object>>) result.get(ResultField.DATA));

      return new ResponseEntity<>(response, HttpStatus.OK);

    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(response, ee);
    }
  }

  @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> clearEventsLog() {
    EventResponse response = new EventResponse();

    try {
      service.clearEventsLog();
      response.setMessage("The events log has been cleaned.");
      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(response, ee);
    }
  }
}
