package pl.databucket.controller;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.database.*;
import pl.databucket.exception.*;
import pl.databucket.service.ViewService;
import pl.databucket.response.BaseResponse;
import pl.databucket.response.ViewResponse;

import java.util.*;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api")
@RestController
public class ViewController {

  private final CustomExceptionFormatter customExceptionFormatter;
  private final ViewService service;

  public ViewController(ViewService service) {
    this.service = service;
    this.customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(ViewController.class));
  }

  @PostMapping(value = "/view", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> createView(@RequestParam String userName, @RequestBody HashMap<String, Object> body) {
    ViewResponse rb = new ViewResponse();

    try {
      String viewName = FieldValidator.validateViewName(body, true);
      Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
      Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
      Integer columnsId = FieldValidator.validateNullableId(body, COL.COLUMNS_ID, false);
      Integer filterId = FieldValidator.validateNullableId(body, COL.FILTER_ID, false);
      String description = FieldValidator.validateDescription(body, false);

      Integer viewId = service.createView(userName, viewName, description, bucketId, classId, columnsId, filterId);

      rb.setViewId(viewId);
      rb.setMessage("The new view has been successfully created.");
      return new ResponseEntity<>(rb, HttpStatus.CREATED);
    } catch (ViewAlreadyExistsException | EmptyInputValueException | ExceededMaximumNumberOfCharactersException e2) {
      return customExceptionFormatter.customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(rb, ee);
    }
  }

  @DeleteMapping(value = "/view/{viewId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> deleteViews(@PathVariable Integer viewId, @RequestParam String userName) {
    ViewResponse rb = new ViewResponse();
    try {
      service.deleteView(userName, viewId);
      rb.setMessage("The view has been removed.");
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(rb, ee);
    }
  }

  @PutMapping(value = "/view/{viewId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> modifyView(@PathVariable("viewId") Integer viewId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
    ViewResponse rb = new ViewResponse();
    try {
      Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
      Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
      Integer columnsId = FieldValidator.validateNullableId(body, COL.COLUMNS_ID, false);
      Integer filterId = FieldValidator.validateNullableId(body, COL.FILTER_ID, false);
      String description = FieldValidator.validateDescription(body, false);
      String viewName = FieldValidator.validateViewName(body, false);

      service.modifyView(userName, viewId, viewName, bucketId, classId, description, columnsId, filterId);
      rb.setMessage("View with id '" + viewId + "' has been successfully modified.");
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e) {
      return customExceptionFormatter.customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(rb, ee);
    }
  }

  @GetMapping(value = {"/view", "/view/{viewId}", "/buckets/{bucketName}/view"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BaseResponse> getViews(
      @PathVariable Optional<String> bucketName,
      @PathVariable Optional<Integer> viewId,
      @RequestParam(required = false) Optional<Integer> page,
      @RequestParam(required = false) Optional<Integer> limit,
      @RequestParam(required = false) Optional<String> sort,
      @RequestParam(required = false) Optional<String> filter) {

    ViewResponse rb = new ViewResponse();
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

      Map<ResultField, Object> result = service.getViews(bucketName, viewId, page, limit, sort, urlConditions);

      long total = (long) result.get(ResultField.TOTAL);
      rb.setTotal(total);

      if (page.isPresent() && limit.isPresent()) {
        rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
      }

      rb.setViews((List<Map<String, Object>>) result.get(ResultField.DATA));

      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e) {
      return customExceptionFormatter.customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (IncorrectValueException e1) {
      return customExceptionFormatter.customException(rb, e1, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return customExceptionFormatter.defaultException(rb, ee);
    }
  }
}
