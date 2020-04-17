package pl.databucket.web.filters;

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
import pl.databucket.exception.*;
import pl.databucket.database.C;
import pl.databucket.database.COL;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.service.DatabucketService;
import pl.databucket.service.ResponseBody;
import pl.databucket.service.ResponseStatus;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/filters")
@RestController
public class FiltersController {

    private static final Logger logger = LoggerFactory.getLogger(FiltersController.class);

    private final DatabucketService databucketService;

    public FiltersController(DatabucketService databucketService) {
        this.databucketService = databucketService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> createFilter(@RequestParam String userName, @RequestBody Map<String, Object> body) {
        ResponseBody rb = new ResponseBody();

        try {
            String filterName = FieldValidator.validateFilterName(body, true);
            Integer bucketId = null;
            if (body.containsKey(COL.BUCKET_ID)) {
                bucketId = (Integer) body.get(COL.BUCKET_ID);
            }

            List<Map<String, Object>> conditions = FieldValidator.validateConditions(body, true);
            String description = FieldValidator.validateDescription(body, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);

            int filterId = databucketService.createFilter(filterName, bucketId, userName, conditions, description, classId);
            rb.setStatus(ResponseStatus.OK);
            rb.setFilterId(filterId);
            rb.setMessage("The new filter has been successfully created.");
            return new ResponseEntity<>(rb, HttpStatus.CREATED);
        } catch (ItemDoNotExistsException e) {
            return customException(rb, e, HttpStatus.NOT_FOUND);
        } catch (EmptyInputValueException | FilterAlreadyExistsException e1) {
            return customException(rb, e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return defaultException(rb, ee);
        }
    }

    @DeleteMapping(value = "/{filterId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> deleteFilters(@PathVariable Integer filterId, @RequestParam String userName) {
        ResponseBody rb = new ResponseBody();
        try {
            databucketService.deleteFilter(userName, filterId);
            rb.setStatus(ResponseStatus.OK);
            rb.setMessage("The filter has been removed.");
            return new ResponseEntity<>(rb, HttpStatus.OK);
        } catch (ItemAlreadyUsedException e1) {
            return customException(rb, e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return defaultException(rb, ee);
        }
    }

    @SuppressWarnings("unchecked")
    @GetMapping(value = {
            "",
            "/{filterId}",
            "/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> getFilters(
            @PathVariable Optional<String> bucketName,
            @PathVariable Optional<Integer> filterId,
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

            Map<String, Object> result = databucketService.getFilters(bucketName, filterId, page, limit, sort, urlConditions);

            long total = (long) result.get(C.TOTAL);
            rb.setTotal(total);

            if (page.isPresent() && limit.isPresent()) {
                rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
            }

            rb.setFilters((List<Map<String, Object>>) result.get(C.FILTERS));

            return new ResponseEntity<>(rb, HttpStatus.OK);
        } catch (ItemDoNotExistsException e) {
            return customException(rb, e, HttpStatus.NOT_FOUND);
        } catch (IncorrectValueException e2) {
            return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return defaultException(rb, ee);
        }
    }

    @PutMapping(value = "/{filterId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> modifyFilter(@PathVariable("filterId") Integer filterId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
        ResponseBody rb = new ResponseBody();
        try {
            Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
            String filterName = FieldValidator.validateFilterName(body, false);
            String description = FieldValidator.validateDescription(body, false);
            List<Map<String, Object>> conditions = FieldValidator.validateConditions(body, false);

            databucketService.modifyFilter(userName, filterId, filterName, bucketId, classId, description, conditions);
            rb.setStatus(ResponseStatus.OK);
            rb.setMessage("Filter with id '" + filterId + "' has been successfully modified.");
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
