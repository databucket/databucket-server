package pl.databucket.controller;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.database.COL;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.database.ResultField;
import pl.databucket.exception.*;
import pl.databucket.service.EventService;
import pl.databucket.response.BaseResponse;
import pl.databucket.response.EventResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/event")
@RestController
public class EventController {

  private final CustomExceptionFormatter customExceptionFormatter;
  private final EventService service;

  public EventController(EventService service) {
    this.service = service;
    this.customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(EventController.class));
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> createEvent(@RequestParam String userName, @RequestBody Map<String, Object> body) {
    EventResponse response = new EventResponse();

    try {
      String eventName = FieldValidator.validateEventName(body, true);
      Boolean active = FieldValidator.validateEventStatus(body, false);
      Map<String, Object> schedule = FieldValidator.validateEventSchedule(body, true, active);
      List<Map<String, Object>> tasks = FieldValidator.validateEventTasks(body, true);
      String description = FieldValidator.validateDescription(body, false);
      Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
      Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);

      int eventId = service.createEvent(eventName, bucketId, classId, userName, description, schedule, tasks, active);
      response.setEventId(eventId);
      response.setMessage("The new event has been successfully created.");
      return new ResponseEntity<>(response, HttpStatus.CREATED);
    } catch (ItemDoNotExistsException e) {
      return customExceptionFormatter.customException(response, e, HttpStatus.NOT_FOUND);
    } catch (EmptyInputValueException e1) {
      return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(response, ee);
    }
  }

  @DeleteMapping(value = "/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> deleteEvent(@PathVariable Integer eventId, @RequestParam String userName) {
    EventResponse response = new EventResponse();
    try {
      service.deleteEvent(userName, eventId);
      response.setMessage("The event has been removed.");
      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(response, ee);
    }
  }

  @GetMapping(value = {
      "",
      "/{eventId}",
      "/bucket/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> getEvents(
      @PathVariable Optional<String> bucketName,
      @PathVariable Optional<Integer> eventId,
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

      Map<ResultField, Object> result = service.getEvents(bucketName, eventId, page, limit, sort, urlConditions);

      long total = (long) result.get(ResultField.TOTAL);
      response.setTotal(total);

      if (page.isPresent() && limit.isPresent()) {
        response.setTotalPages((int) Math.ceil(total / (float) limit.get()));
      }

      response.setEvents((List<Map<String, Object>>) result.get(ResultField.DATA));

      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (IncorrectValueException e2) {
      return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(response, ee);
    }
  }

  @PutMapping(value = "/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> modifyEvent(@PathVariable("eventId") Integer eventId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
    EventResponse response = new EventResponse();
    try {
      Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
      Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
      String eventName = FieldValidator.validateEventName(body, false);
      String description = FieldValidator.validateDescription(body, false);
      List<Map<String, Object>> tasks = FieldValidator.validateEventTasks(body, false);
      Boolean active = FieldValidator.validateEventStatus(body, false);
      Map<String, Object> schedule = FieldValidator.validateEventSchedule(body, false, active);

      service.modifyEvent(userName, eventId, eventName, bucketId, classId, description, schedule, tasks, active);
      response.setMessage("Event with id '" + eventId + "' has been successfully modified.");
      return new ResponseEntity<>(response, HttpStatus.OK);
    } catch (ItemDoNotExistsException e1) {
      return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_FOUND);
    } catch (EmptyInputValueException | IncorrectValueException | ExceededMaximumNumberOfCharactersException e2) {
      return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(response, ee);
    }
  }

}
