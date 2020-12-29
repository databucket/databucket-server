package pl.databucket.controller;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.database.*;
import pl.databucket.exception.*;
import pl.databucket.service.BucketService;
import pl.databucket.response.BaseResponse;
import pl.databucket.response.BucketResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/bucket")
@RestController
public class BucketController {

    private final CustomExceptionFormatter customExceptionFormatter;
    private final BucketService service;

    @Autowired
    public BucketController(BucketService service) {
        this.service = service;
        this.customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(BucketController.class));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> createBucket(
            @RequestParam String userName,
            @RequestBody LinkedHashMap<String, Object> body) {

        BucketResponse response = new BucketResponse();

        try {
            String bucketName = FieldValidator.validateBucketName(body, true);
            Integer index = FieldValidator.validateIndex(body, false);
            String description = FieldValidator.validateDescription(body, false);
            String iconName = FieldValidator.validateIcon(body, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);

            boolean history = false;
            if (body.containsKey(COL.HISTORY)) {
                history = (boolean) body.get(COL.HISTORY);
            }

            int bucketId = service.createBucket(userName, bucketName, index, description, iconName, history, classId);
            response.setBucketId(bucketId);
            response.setMessage("The bucket '" + bucketName + "' has been successfully created.");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (BucketAlreadyExistsException | ExceededMaximumNumberOfCharactersException | EmptyInputValueException | IncorrectValueException e) {
            return customExceptionFormatter.customException(response, e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @DeleteMapping(value = "/{bucketName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> deleteBucket(
            @PathVariable("bucketName") String bucketName,
            @RequestParam String userName) {

        BucketResponse response = new BucketResponse();
        try {
            service.deleteBucket(bucketName, userName);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemAlreadyUsedException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (ItemDoNotExistsException e2) {
            return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @GetMapping(value = {"", "/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> getBuckets(
            @PathVariable Optional<String> bucketName,
            @RequestParam(required = false) Optional<Integer> page,
            @RequestParam(required = false) Optional<Integer> limit,
            @RequestParam(required = false) Optional<String> sort,
            @RequestParam(required = false) Optional<String> filter) {

        BucketResponse response = new BucketResponse();

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

            Map<ResultField, Object> result = service.getBuckets(bucketName, page, limit, sort, urlConditions);

            long total = (long) result.get(ResultField.TOTAL);
            response.setTotal(total);

            if (page.isPresent() && limit.isPresent()) {
                response.setTotalPages((int) Math.ceil(total / (float) limit.get()));
            }

            response.setBuckets((List<Map<String, Object>>) result.get(ResultField.DATA));

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IncorrectValueException e) {
            return customExceptionFormatter.customException(response, e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @PutMapping(value = "/{bucketName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> modifyBucket(
            @PathVariable("bucketName") String bucketName,
            @RequestParam String userName,
            @RequestBody LinkedHashMap<String, Object> body) {

        BucketResponse response = new BucketResponse();

        try {
            service.modifyBucket(userName, bucketName, body);
            response.setMessage("Bucket '" + bucketName + "' has been successfully modified.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemDoNotExistsException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_FOUND);
        } catch (BucketAlreadyExistsException | ExceededMaximumNumberOfCharactersException | IncorrectValueException e2) {
            return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }
}
