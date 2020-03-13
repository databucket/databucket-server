package pl.databucket.web.views;

import java.util.HashMap;
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
import pl.databucket.exception.ViewAlreadyExistsException;
import pl.databucket.database.C;
import pl.databucket.database.COL;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.service.DatabucketService;
import pl.databucket.service.ResponseBody;
import pl.databucket.service.ResponseStatus;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api")
@RestController
public class ViewsController {

  private static final Logger logger = LoggerFactory.getLogger(ViewsController.class);

  private final DatabucketService databucketService;

  public ViewsController(DatabucketService databucketService) {
    this.databucketService = databucketService;
  }

  @PostMapping(value = "/views", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> createView(@RequestParam String userName, @RequestBody HashMap<String, Object> body) {
    ResponseBody rb = new ResponseBody();

    try {
      String viewName = FieldValidator.validateViewName(body, true);
      Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
      Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
      Integer columnsId = FieldValidator.validateNullableId(body, COL.COLUMNS_ID, false);
      Integer filterId = FieldValidator.validateNullableId(body, COL.FILTER_ID, false);
      String description = FieldValidator.validateDescription(body, false);

      Integer viewId = databucketService.createView(userName, viewName, description, bucketId, classId, columnsId, filterId);

      rb.setViewId(viewId);
      rb.setMessage("The new view has been successfully created.");
      rb.setStatus(ResponseStatus.OK);
      return new ResponseEntity<>(rb, HttpStatus.CREATED);
    } catch (ViewAlreadyExistsException | EmptyInputValueException | ExceededMaximumNumberOfCharactersException e2) {
      return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @DeleteMapping(value = "/views/{viewId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> deleteViews(@PathVariable Integer viewId, @RequestParam String userName) {
    ResponseBody rb = new ResponseBody();
    try {
      databucketService.deleteView(userName, viewId);
      rb.setStatus(ResponseStatus.OK);
      rb.setMessage("The view has been removed.");
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @PutMapping(value = "/views/{viewId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> modifyView(@PathVariable("viewId") Integer viewId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
    ResponseBody rb = new ResponseBody();
    try {
      Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
      Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
      Integer columnsId = FieldValidator.validateNullableId(body, COL.COLUMNS_ID, false);
      Integer filterId = FieldValidator.validateNullableId(body, COL.FILTER_ID, false);
      String description = FieldValidator.validateDescription(body, false);
      String viewName = FieldValidator.validateViewName(body, false);

      databucketService.modifyView(userName, viewId, viewName, bucketId, classId, description, columnsId, filterId);
      rb.setStatus(ResponseStatus.OK);
      rb.setMessage("View with id '" + viewId + "' has been successfully modified.");
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @SuppressWarnings("unchecked")
  @GetMapping(value = {"/views", "/views/{viewId}", "/buckets/{bucketName}/views"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> getViews(
      @PathVariable Optional<String> bucketName,
      @PathVariable Optional<Integer> viewId,
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

      Map<String, Object> result = databucketService.getViews(bucketName, viewId, page, limit, sort, urlConditions);

      long total = (long) result.get(C.TOTAL);
      rb.setTotal(total);

      if (page.isPresent() && limit.isPresent()) {
        rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
      }

      rb.setViews((List<Map<String, Object>>) result.get(C.VIEWS));

      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (IncorrectValueException e1) {
      return customException(rb, e1, HttpStatus.NOT_ACCEPTABLE);
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
