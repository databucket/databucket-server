package pl.databucket.controller;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.database.*;
import pl.databucket.exception.*;
import pl.databucket.service.FilterService;
import pl.databucket.response.BaseResponse;
import pl.databucket.response.FilterResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/filter")
@RestController
public class FilterController {

    private final CustomExceptionFormatter customExceptionFormatter;
    private final FilterService service;

    public FilterController(FilterService service) {
        this.service = service;
        this.customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(FilterController.class));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> createFilter(@RequestParam String userName, @RequestBody Map<String, Object> body) {
        FilterResponse response = new FilterResponse();

        try {
            String filterName = FieldValidator.validateFilterName(body, true);
            Integer bucketId = null;
            if (body.containsKey(COL.BUCKET_ID)) {
                bucketId = (Integer) body.get(COL.BUCKET_ID);
            }

            List<Map<String, Object>> conditions = FieldValidator.validateConditions(body, true);
            String description = FieldValidator.validateDescription(body, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);

            int filterId = service.createFilter(filterName, bucketId, userName, conditions, description, classId);
            response.setFilterId(filterId);
            response.setMessage("The new filter has been successfully created.");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ItemDoNotExistsException e) {
            return customExceptionFormatter.customException(response, e, HttpStatus.NOT_FOUND);
        } catch (EmptyInputValueException | FilterAlreadyExistsException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @DeleteMapping(value = "/{filterId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> deleteFilters(@PathVariable Integer filterId, @RequestParam String userName) {
        FilterResponse response = new FilterResponse();
        try {
            service.deleteFilter(userName, filterId);
            response.setMessage("The filter has been removed.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemAlreadyUsedException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @GetMapping(value = {
            "",
            "/{filterId}",
            "/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> getFilters(
            @PathVariable Optional<String> bucketName,
            @PathVariable Optional<Integer> filterId,
            @RequestParam(required = false) Optional<Integer> page,
            @RequestParam(required = false) Optional<Integer> limit,
            @RequestParam(required = false) Optional<String> sort,
            @RequestParam(required = false) Optional<String> filter) {

        FilterResponse response = new FilterResponse();
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

            Map<ResultField, Object> result = service.getFilters(bucketName, filterId, page, limit, sort, urlConditions);

            long total = (long) result.get(ResultField.TOTAL);
            response.setTotal(total);

            if (page.isPresent() && limit.isPresent()) {
                response.setTotalPages((int) Math.ceil(total / (float) limit.get()));
            }

            response.setFilters((List<Map<String, Object>>) result.get(ResultField.DATA));

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemDoNotExistsException e) {
            return customExceptionFormatter.customException(response, e, HttpStatus.NOT_FOUND);
        } catch (IncorrectValueException e2) {
            return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @PutMapping(value = "/{filterId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> modifyFilter(@PathVariable("filterId") Integer filterId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
        FilterResponse response = new FilterResponse();
        try {
            Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
            String filterName = FieldValidator.validateFilterName(body, false);
            String description = FieldValidator.validateDescription(body, false);
            List<Map<String, Object>> conditions = FieldValidator.validateConditions(body, false);

            service.modifyFilter(userName, filterId, filterName, bucketId, classId, description, conditions);
            response.setMessage("Filter with id '" + filterId + "' has been successfully modified.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemDoNotExistsException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_FOUND);
        } catch (EmptyInputValueException | ExceededMaximumNumberOfCharactersException e2) {
            return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }
}
