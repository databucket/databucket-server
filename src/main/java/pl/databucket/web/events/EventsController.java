package pl.databucket.web.events;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.exception.EmptyInputValueException;
import pl.databucket.exception.ExceededMaximumNumberOfCharactersException;
import pl.databucket.exception.IncorrectValueException;
import pl.databucket.exception.ItemDoNotExistsException;
import pl.databucket.database.C;
import pl.databucket.database.COL;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.service.DatabucketService;
import pl.databucket.service.ResponseBody;
import pl.databucket.service.ResponseStatus;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/events")
@RestController
public class EventsController {

  private static final Logger logger = LoggerFactory.getLogger(EventsController.class);

  private final DatabucketService databucketService;

  public EventsController(DatabucketService databucketService) {
    this.databucketService = databucketService;
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> createEvent(@RequestParam String userName, @RequestBody Map<String, Object> body) {
    ResponseBody rb = new ResponseBody();

    try {
      String eventName = FieldValidator.validateEventName(body, true);
      Boolean active = FieldValidator.validateEventStatus(body, false);
      Map<String, Object> schedule = FieldValidator.validateEventSchedule(body, true, active);
      List<Map<String, Object>> tasks = FieldValidator.validateEventTasks(body, true);
      String description = FieldValidator.validateDescription(body, false);
      Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
      Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);

      int eventId = databucketService.createEvent(eventName, bucketId, classId, userName, description, schedule, tasks, active);
      rb.setStatus(ResponseStatus.OK);
      rb.setEventId(eventId);
      rb.setMessage("The new event has been successfully created.");
      return new ResponseEntity<>(rb, HttpStatus.CREATED);
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (EmptyInputValueException e1) {
      return customException(rb, e1, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @DeleteMapping(value = "/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> deleteEvent(@PathVariable Integer eventId, @RequestParam String userName) {
    ResponseBody rb = new ResponseBody();
    try {
      databucketService.deleteEvent(userName, eventId);
      rb.setStatus(ResponseStatus.OK);
      rb.setMessage("The event has been removed.");
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @SuppressWarnings("unchecked")
  @GetMapping(value = {
      "",
      "/{eventId}",
      "/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> getEvents(
      @PathVariable Optional<String> bucketName,
      @PathVariable Optional<Integer> eventId,
      @RequestParam(required = false) Optional<Integer> page,
      @RequestParam(required = false) Optional<Integer> limit,
      @RequestParam(required = false) Optional<String> sort,
      @RequestParam(required = false) Optional<String> filter) {

    ResponseBody rb = new ResponseBody();
    try {
      if (page.isPresent()) {
        FieldValidator.mustBeGraterThen0("page", page.get());
        rb.setPage(page.get());
      }

      if (limit.isPresent()) {
        FieldValidator.mustBeGraterOrEqual0("limit", limit.get());
        rb.setLimit(limit.get());
      }

      if (sort.isPresent()) {
        FieldValidator.validateSort(sort.get());
        rb.setSort(sort.get());
      }

      List<Condition> urlConditions = null;
      if (filter.isPresent()) {
        urlConditions = FieldValidator.validateFilter(filter.get());
      }

      Map<String, Object> result = databucketService.getEvents(bucketName, eventId, page, limit, sort, urlConditions);

      long total = (long) result.get(C.TOTAL);
      rb.setTotal(total);

      if (page.isPresent() && limit.isPresent()) {
        rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
      }

      rb.setEvents((List<Map<String, Object>>) result.get(C.EVENTS));

      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (IncorrectValueException e2) {
      return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @PutMapping(value = "/{eventId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> modifyEvent(@PathVariable("eventId") Integer eventId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
    ResponseBody rb = new ResponseBody();
    try {
      Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
      Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
      String eventName = FieldValidator.validateEventName(body, false);
      String description = FieldValidator.validateDescription(body, false);
      List<Map<String, Object>> tasks = FieldValidator.validateEventTasks(body, false);
      Boolean active = FieldValidator.validateEventStatus(body, false);
      Map<String, Object> schedule = FieldValidator.validateEventSchedule(body, false, active);

      databucketService.modifyEvent(userName, eventId, eventName, bucketId, classId, description, schedule, tasks, active);
      rb.setStatus(ResponseStatus.OK);
      rb.setMessage("Event with id '" + eventId + "' has been successfully modified.");
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e1) {
      return customException(rb, e1, HttpStatus.NOT_FOUND);
    } catch (EmptyInputValueException | IncorrectValueException | ExceededMaximumNumberOfCharactersException e2) {
      return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  private ResponseEntity<ResponseBody> defaultException(ResponseBody rb, Exception e) {
    logger.error("ERROR:", e);
    rb.setStatus(ResponseStatus.FAILED);
    rb.setMessage(e.getMessage());
    return new ResponseEntity<>(rb, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<ResponseBody> customException(ResponseBody rb, Exception e, HttpStatus status) {
    logger.warn(e.getMessage());
    rb.setStatus(ResponseStatus.FAILED);
    rb.setMessage(e.getMessage());
    return new ResponseEntity<>(rb, status);
  }
}
