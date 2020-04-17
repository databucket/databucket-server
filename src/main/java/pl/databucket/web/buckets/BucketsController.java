package pl.databucket.web.buckets;

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
import pl.databucket.exception.*;
import pl.databucket.database.C;
import pl.databucket.database.COL;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.service.DatabucketService;
import pl.databucket.service.ResponseBody;
import pl.databucket.service.ResponseStatus;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/buckets")
@RestController
public class BucketsController {

    private static final Logger logger = LoggerFactory.getLogger(BucketsController.class);

    private final DatabucketService databucketService;

    @Autowired
    public BucketsController(DatabucketService databucketService) {
        this.databucketService = databucketService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> createBucket(@RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
        ResponseBody rb = new ResponseBody();

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

            int bucketId = databucketService.createBucket(userName, bucketName, index, description, iconName, history, classId);
            rb.setStatus(ResponseStatus.OK);
            rb.setBucketId(bucketId);
            rb.setMessage("The bucket '" + bucketName + "' has been successfully created.");
            return new ResponseEntity<>(rb, HttpStatus.CREATED);
        } catch (BucketAlreadyExistsException | ExceededMaximumNumberOfCharactersException | EmptyInputValueException | IncorrectValueException e) {
            return customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return defaultException(rb, ee);
        }
    }

    @DeleteMapping(value = "/{bucketName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> deleteBucket(@PathVariable("bucketName") String bucketName, @RequestParam String userName) {
        ResponseBody rb = new ResponseBody();
        try {
            databucketService.deleteBucket(bucketName, userName);
            rb.setStatus(ResponseStatus.OK);
            return new ResponseEntity<>(rb, HttpStatus.OK);
        } catch (ItemAlreadyUsedException e1) {
            return customException(rb, e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (ItemDoNotExistsException e2) {
            return customException(rb, e2, HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return defaultException(rb, ee);
        }
    }

    @SuppressWarnings("unchecked")
    @GetMapping(value = {"", "/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> getBuckets(
            @PathVariable Optional<String> bucketName,
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

            Map<String, Object> result = databucketService.getBuckets(bucketName, page, limit, sort, urlConditions);

            long total = (long) result.get(C.TOTAL);
            rb.setTotal(total);

            if (page.isPresent() && limit.isPresent()) {
                rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
            }

            rb.setBuckets((List<Map<String, Object>>) result.get(C.BUCKETS));

            return new ResponseEntity<>(rb, HttpStatus.OK);
        } catch (IncorrectValueException e) {
            return customException(rb, e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return defaultException(rb, ee);
        }
    }

    @GetMapping(value = "/{bucketName}/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> getBucketInfo(@PathVariable String bucketName) {
        ResponseBody rb = new ResponseBody();

        try {
            rb.setStatistic(databucketService.getStatistic(bucketName));
            rb.setStatus(ResponseStatus.OK);
            return new ResponseEntity<>(rb, HttpStatus.OK);
        } catch (ItemDoNotExistsException e) {
            return customException(rb, e, HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return defaultException(rb, ee);
        }
    }

    @PutMapping(value = "/{bucketName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> modifyBucket(@PathVariable("bucketName") String bucketName, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {

        ResponseBody rb = new ResponseBody();

        try {
            databucketService.modifyBucket(userName, bucketName, body);
            rb.setStatus(ResponseStatus.OK);
            rb.setMessage("Bucket '" + bucketName + "' has been successfully modified.");
            return new ResponseEntity<>(rb, HttpStatus.OK);
        } catch (ItemDoNotExistsException e1) {
            return customException(rb, e1, HttpStatus.NOT_FOUND);
        } catch (BucketAlreadyExistsException | ExceededMaximumNumberOfCharactersException | IncorrectValueException e2) {
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
