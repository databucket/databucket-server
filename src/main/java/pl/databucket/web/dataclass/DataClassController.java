package pl.databucket.web.dataclass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import pl.databucket.exception.ClassAlreadyExistsException;
import pl.databucket.exception.EmptyInputValueException;
import pl.databucket.exception.ExceededMaximumNumberOfCharactersException;
import pl.databucket.exception.IncorrectValueException;
import pl.databucket.exception.ItemDoNotExistsException;
import pl.databucket.database.C;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.service.DatabucketService;
import pl.databucket.service.ResponseBody;
import pl.databucket.service.ResponseStatus;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/classes")
@RestController
public class DataClassController {

  private static final Logger logger = LoggerFactory.getLogger(DataClassController.class);

  private final DatabucketService databucketService;

  @Autowired
  public DataClassController(DatabucketService databucketService) {
    this.databucketService = databucketService;
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> createClass(@RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
    ResponseBody rb = new ResponseBody();

    try {
      String className = FieldValidator.validateClassName(body, true);
      String description = FieldValidator.validateDescription(body, false);

      int classId = databucketService.createClass(userName, className, description);
      rb.setStatus(ResponseStatus.OK);
      rb.setClassId(classId);
      rb.setMessage("The class '" + className + "' has been successfully created.");
      return new ResponseEntity<>(rb, HttpStatus.CREATED);
    } catch (ClassAlreadyExistsException | ExceededMaximumNumberOfCharactersException | EmptyInputValueException e) {
      return customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @DeleteMapping(value = "/{classId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> deleteClass(@PathVariable("classId") Integer classId, @RequestParam String userName) {
    ResponseBody rb = new ResponseBody();
    try {
      databucketService.deleteClass(classId, userName);
      rb.setStatus(ResponseStatus.OK);
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @SuppressWarnings("unchecked")
  @GetMapping(value = {"", "/{classId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> getClasses(
      @PathVariable Optional<Integer> classId,
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

      Map<String, Object> result = databucketService.getClasses(classId, page, limit, sort, urlConditions);

      long total = (long) result.get(C.TOTAL);
      rb.setTotal(total);

      if (page.isPresent() && limit.isPresent()) {
        rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
      }

      rb.setClasses((List<Map<String, Object>>) result.get(C.CLASSES));

      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (IncorrectValueException e) {
      return customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @PutMapping(value = "/{classId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> modifyClass(@PathVariable("classId") Integer classId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {

    ResponseBody rb = new ResponseBody();

    try {
      databucketService.modifyClass(userName, classId, body);
      rb.setStatus(ResponseStatus.OK);
      rb.setMessage("Class '" + classId + "' has been successfully modified.");
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e1) {
      return customException(rb, e1, HttpStatus.NOT_FOUND);
    } catch (ClassAlreadyExistsException | ExceededMaximumNumberOfCharactersException | IncorrectValueException e2) {
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
