package pl.databucket.web.columns;

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
import pl.databucket.exception.ColumnsAlreadyExistsException;
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
@RequestMapping("/api/columns")
@RestController
public class ColumnsController {

  private static final Logger logger = LoggerFactory.getLogger(ColumnsController.class);


  private final DatabucketService databucketService;

  public ColumnsController(DatabucketService databucketService) {
    this.databucketService = databucketService;
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> createColumns(@RequestParam String userName, @RequestBody Map<String, Object> body) {
    ResponseBody rb = new ResponseBody();

    try {
      String columnsName = FieldValidator.validateColumnsName(body, true);
      Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
      List<Map<String, Object>> columns = FieldValidator.validateColumns(body, true);
      String description = FieldValidator.validateDescription(body, false);
      Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);

      int columnsId = databucketService.createColumns(columnsName, bucketId, userName, columns, description, classId);
      rb.setStatus(ResponseStatus.OK);
      rb.setColumnsId(columnsId);
      rb.setMessage("The new columns definition has been successfully created.");
      return new ResponseEntity<>(rb, HttpStatus.CREATED);
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (EmptyInputValueException | ColumnsAlreadyExistsException e1) {
      return customException(rb, e1, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @DeleteMapping(value = "/{columnsId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> deleteColumns(@PathVariable Integer columnsId, @RequestParam String userName) {
    ResponseBody rb = new ResponseBody();
    try {
      databucketService.deleteColumns(userName, columnsId);
      rb.setStatus(ResponseStatus.OK);
      rb.setMessage("The columns definition has been removed.");
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @SuppressWarnings("unchecked")
  @GetMapping(value = {
      "",
      "/{columnsId}",
      "/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> getColumns(
      @PathVariable Optional<String> bucketName,
      @PathVariable Optional<Integer> columnsId,
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

      Map<String, Object> result = databucketService.getColumns(bucketName, columnsId, page, limit, sort, urlConditions);

      long total = (long) result.get(C.TOTAL);
      rb.setTotal(total);

      if (page.isPresent() && limit.isPresent()) {
        rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
      }

      rb.setColumns((List<Map<String, Object>>) result.get(C.COLUMNS));

      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (IncorrectValueException e2) {
      return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @PutMapping(value = "/{columnsId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> modifyColumns(@PathVariable("columnsId") Integer columnsId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
    ResponseBody rb = new ResponseBody();
    try {
      Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
      Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
      String columnsName = FieldValidator.validateColumnsName(body, false);
      String description = FieldValidator.validateDescription(body, false);
      List<Map<String, Object>> columns = FieldValidator.validateColumns(body, false);

      databucketService.modifyColumns(userName, columnsId, columnsName, bucketId, classId, description, columns);
      rb.setStatus(ResponseStatus.OK);
      rb.setMessage("Columns with id '" + columnsId + "' have been successfully modified.");
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e1) {
      return customException(rb, e1, HttpStatus.NOT_FOUND);
    } catch (EmptyInputValueException | ExceededMaximumNumberOfCharactersException e2) {
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
