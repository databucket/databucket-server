package pl.databucket.web.bundles;

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
import pl.databucket.exception.IncorrectValueException;
import pl.databucket.exception.ItemDoNotExistsException;
import pl.databucket.exception.UnknownColumnException;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.service.DatabucketService;
import pl.databucket.service.ResponseBody;
import pl.databucket.service.ResponseStatus;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api")
@RestController
public class BundlesController {

  private static final Logger logger = LoggerFactory.getLogger(BundlesController.class);

  private final DatabucketService databucketService;

  public BundlesController(DatabucketService databucketService) {
    this.databucketService = databucketService;
  }

  @SuppressWarnings("unchecked")
  @PostMapping(value = "/buckets/{bucketName}/bundles/custom", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> getBundlesCustom(
      @PathVariable String bucketName,
      @RequestParam(required = false) Optional<Integer> page,
      @RequestParam(required = false) Optional<Integer> limit,
      @RequestParam(required = false) Optional<String> sort,
      @RequestBody LinkedHashMap<String, Object> body) {

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

      List<Map<String, Object>> columns = FieldValidator.validateColumns(body, false);
      List<Condition> conditions = FieldValidator.validateListOfConditions(body, false);

      Map<String, Object> result = databucketService
          .getBundles(bucketName, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(columns), Optional.ofNullable(conditions), page, limit, sort);

      long total = (long) result.get("total");
      rb.setTotal(total);

      if (page.isPresent() && limit.isPresent()) {
        rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
      }

      rb.setBundles((List<Map<String, Object>>) result.get("bundles"));

      if (rb.getBundles().size() > 0) {
        return new ResponseEntity<>(rb, HttpStatus.OK);
      } else {
        if (limit.get() > 0) {
          rb.setMessage("No bundle meets the given requirements!");
          rb.setStatus(ResponseStatus.NO_DATA);
        }
        return new ResponseEntity<>(rb, HttpStatus.OK);
      }
    } catch (ItemDoNotExistsException e1) {
      return customException(rb, e1, HttpStatus.NOT_FOUND);
    } catch (IncorrectValueException | UnknownColumnException e2) {
      return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @SuppressWarnings("unchecked")
  @GetMapping(value = {
      "/buckets/{bucketName}/bundles",
      "/buckets/{bucketName}/bundles/{bundleId}",
      "/buckets/{bucketName}/bundles/tags/{tagId}",
      "/buckets/{bucketName}/bundles/filters/{filterId}",
      "/buckets/{bucketName}/bundles/views/{viewId}"},
      produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> getBundles(
      @PathVariable String bucketName,
      @PathVariable(required = false) Optional<Integer[]> bundleId,
      @PathVariable(required = false) Optional<Integer[]> tagId,
      @PathVariable(required = false) Optional<Integer> filterId,
      @PathVariable(required = false) Optional<Integer> viewId,
      @RequestParam(required = false) Optional<Integer> page,
      @RequestParam(required = false) Optional<Integer> limit,
      @RequestParam(required = false) Optional<String> sort) {

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

      Map<String, Object> result = databucketService.getBundles(bucketName, bundleId, tagId, filterId, viewId, Optional.empty(), Optional.empty(), page, limit, sort);

      long total = (long) result.get("total");
      rb.setTotal(total);

      if (page.isPresent() && limit.isPresent()) {
        rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
      }

      rb.setBundles((List<Map<String, Object>>) result.get("bundles"));

      if (rb.getBundles().size() > 0) {
        return new ResponseEntity<>(rb, HttpStatus.OK);
      } else {
        rb.setMessage("No bundle meets the given requirements!");
        rb.setStatus(ResponseStatus.NO_DATA);
        return new ResponseEntity<>(rb, HttpStatus.OK);
      }
    } catch (ItemDoNotExistsException e1) {
      return customException(rb, e1, HttpStatus.NOT_FOUND);
    } catch (IncorrectValueException | UnknownColumnException e2) {
      return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @GetMapping(value = {
      "/buckets/{bucketName}/bundles/lock",
      "/buckets/{bucketName}/bundles/tags/{tagId}/lock",
      "/buckets/{bucketName}/bundles/filters/{filterId}/lock"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> lockBundles(
      @PathVariable String bucketName,
      @PathVariable(required = false) Optional<Integer[]> tagId,
      @PathVariable(required = false) Optional<Integer> filterId,
      @RequestParam String userName,
      @RequestParam(required = false) Optional<Integer> page,
      @RequestParam(required = false) Optional<Integer> limit,
      @RequestParam(required = false) Optional<String> sort) {

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

      List<Integer> bundlesIdsList = databucketService.lockBundles(bucketName, userName, tagId, filterId, Optional.empty(), page, limit, sort);

      if (bundlesIdsList != null && bundlesIdsList.size() > 0) {
        Integer[] bundlesIds = bundlesIdsList.toArray(new Integer[bundlesIdsList.size()]);
        return getBundles(bucketName, Optional.of(bundlesIds), Optional.empty(), Optional.empty(), Optional.empty(), page, limit, sort);
      } else {
        rb.setMessage("No bundle meets the given requirements!");
        rb.setBundlesIds(null);
        rb.setStatus(ResponseStatus.NO_DATA);
        return new ResponseEntity<>(rb, HttpStatus.OK);
      }
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (IncorrectValueException | UnknownColumnException e2) {
      return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  // Lock bundles with given conditions
  @PostMapping(value = {"/buckets/{bucketName}/bundles/custom/lock"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> lockBundles(
      @PathVariable("bucketName") String bucketName,
      @RequestParam String userName,
      @RequestParam(required = false) Optional<Integer> page,
      @RequestParam(required = false) Optional<Integer> limit,
      @RequestParam(required = false) Optional<String> sort,
      @RequestBody LinkedHashMap<String, Object> body) {

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

      List<Condition> conditions = FieldValidator.validateListOfConditions(body, true);

      List<Integer> bundlesIdsList = databucketService.lockBundles(bucketName, userName, Optional.empty(), Optional.empty(), Optional.of(conditions), page, limit, sort);
      if (bundlesIdsList != null && bundlesIdsList.size() > 0) {
        Integer[] bundlesIds = bundlesIdsList.toArray(new Integer[bundlesIdsList.size()]);
        return getBundles(bucketName, Optional.of(bundlesIds), Optional.empty(), Optional.empty(), Optional.empty(), page, limit, sort);
      } else {
        rb.setMessage("No bundle meets the given requirements!");
        rb.setBundlesIds(null);
        rb.setStatus(ResponseStatus.NO_DATA);
        return new ResponseEntity<>(rb, HttpStatus.OK);
      }
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (IncorrectValueException | UnknownColumnException e2) {
      return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @PostMapping(value = "/buckets/{bucketName}/bundles", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> createBundle(
      @PathVariable("bucketName") String bucketName,
      @RequestParam String userName,
      @RequestBody Map<String, Object> body) {

    ResponseBody rb = new ResponseBody();
    try {
      Integer bundleId = databucketService.createBundle(userName, bucketName, body);
      rb.setStatus(ResponseStatus.OK);
      rb.setBundleId(bundleId);
      rb.setMessage("The new bundle has been successfully created.");
      return new ResponseEntity<>(rb, HttpStatus.CREATED);
    } catch (ItemDoNotExistsException e1) {
      return customException(rb, e1, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @PutMapping(value = {
      "/buckets/{bucketName}/bundles",
      "/buckets/{bucketName}/bundles/{bundlesIds}",
      "/buckets/{bucketName}/bundles/filters/{filterId}",
      "/buckets/{bucketName}/bundles/tags/{tagsIds}"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> modifyBundles(
      @PathVariable String bucketName,
      @PathVariable Optional<Integer[]> bundlesIds,
      @PathVariable Optional<Integer> filterId,
      @PathVariable Optional<Integer[]> tagsIds,
      @RequestParam(required = true) String userName,
      @RequestBody(required = true) LinkedHashMap<String, Object> body) {
    ResponseBody rb = new ResponseBody();
    try {
      int count = databucketService.modifyBundles(userName, bucketName, bundlesIds, filterId, tagsIds, body);
      rb.setStatus(ResponseStatus.OK);
      rb.setMessage("Number of modified bundles: " + count);
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e1) {
      return customException(rb, e1, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @PutMapping(value = "/buckets/{bucketName}/bundles/custom", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> modifyBundlesCustom(@PathVariable String bucketName, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
    ResponseBody rb = new ResponseBody();
    try {
      int count = databucketService.modifyBundles(userName, bucketName, Optional.empty(), Optional.empty(), Optional.empty(), body);
      rb.setStatus(ResponseStatus.OK);
      rb.setMessage("Number of modified bundles: " + count);
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e1) {
      return customException(rb, e1, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @DeleteMapping(value = {
      "/buckets/{bucketName}/bundles",
      "/buckets/{bucketName}/bundles/{bundlesIds}",
      "/buckets/{bucketName}/bundles/filters/{filterId}",
      "/buckets/{bucketName}/bundles/tags/{tagsIds}"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> deleteBundles(
      @PathVariable String bucketName,
      @PathVariable Optional<Integer[]> bundlesIds,
      @PathVariable Optional<Integer> filterId,
      @PathVariable Optional<Integer[]> tagsIds) {

    ResponseBody rb = new ResponseBody();
    try {
      int count = databucketService.deleteBundles(bucketName, bundlesIds, filterId, tagsIds);
      rb.setStatus(ResponseStatus.OK);
      rb.setMessage("Number of removed bundles: " + count);
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
    } catch (Exception ee) {
      return defaultException(rb, ee);
    }
  }

  @DeleteMapping(value = {"/buckets/{bucketName}/bundles/custom"}, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResponseBody> deleteBundlesCustom(@PathVariable String bucketName, @RequestBody LinkedHashMap<String, Object> body) {

    ResponseBody rb = new ResponseBody();
    try {
      List<Condition> conditions = FieldValidator.validateListOfConditions(body, false);
      int count = databucketService.deleteBundles(bucketName, conditions);
      rb.setStatus(ResponseStatus.OK);
      rb.setMessage("Number of removed bundles: " + count);
      return new ResponseEntity<>(rb, HttpStatus.OK);
    } catch (ItemDoNotExistsException e) {
      return customException(rb, e, HttpStatus.NOT_FOUND);
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
