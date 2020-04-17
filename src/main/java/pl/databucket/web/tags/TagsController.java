package pl.databucket.web.tags;

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
@RequestMapping("/api/tags")
@RestController
public class TagsController {

    private static final Logger logger = LoggerFactory.getLogger(TagsController.class);

    private final DatabucketService databucketService;

    public TagsController(DatabucketService databucketService) {
        this.databucketService = databucketService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> createTag(@RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {

        ResponseBody rb = new ResponseBody();

        try {
            Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
            String bucketName = FieldValidator.validateBucketName(body, false);
            String tagName = FieldValidator.validateTagName(body, true);
            String description = FieldValidator.validateDescription(body, false);
            String iconName = FieldValidator.validateIcon(body, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);

            Integer tagId = databucketService.createTag(userName, tagName, bucketId, bucketName, iconName, description, classId);
            rb.setStatus(ResponseStatus.OK);
            rb.setTagId(tagId);
            rb.setMessage("The new tag has been successfully created.");
            return new ResponseEntity<>(rb, HttpStatus.CREATED);
        } catch (ItemDoNotExistsException e1) {
            return customException(rb, e1, HttpStatus.NOT_FOUND);
        } catch (EmptyInputValueException | ExceededMaximumNumberOfCharactersException | IncorrectValueException | TagAlreadyExistsException e2) {
            return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return defaultException(rb, ee);
        }
    }

    @DeleteMapping(value = "/{tagId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> deleteTags(@PathVariable Integer tagId, @RequestParam String userName) {

        ResponseBody rb = new ResponseBody();

        try {
            databucketService.deleteTag(userName, tagId);
            rb.setStatus(ResponseStatus.OK);
            rb.setMessage("The tag has been removed.");
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
            "/{tagId}",
            "/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> getTags(
            @PathVariable Optional<String> bucketName,
            @PathVariable Optional<Integer> tagId,
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

            Map<String, Object> result = databucketService.getTags(bucketName, tagId, page, limit, sort, urlConditions);

            long total = (long) result.get(C.TOTAL);
            rb.setTotal(total);

            if (page.isPresent() && limit.isPresent()) {
                rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
            }

            rb.setTags((List<Map<String, Object>>) result.get(C.TAGS));

            return new ResponseEntity<>(rb, HttpStatus.OK);
        } catch (ItemDoNotExistsException e1) {
            return customException(rb, e1, HttpStatus.NOT_FOUND);
        } catch (IncorrectValueException e2) {
            return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return defaultException(rb, ee);
        }
    }

    @PutMapping(value = "/{tagId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> modifyTag(@PathVariable("tagId") Integer tagId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {

        ResponseBody rb = new ResponseBody();

        try {
            Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
            String tagName = FieldValidator.validateTagName(body, false);
            String description = FieldValidator.validateDescription(body, false);

            databucketService.modifyTag(userName, tagId, tagName, bucketId, classId, description);
            rb.setStatus(ResponseStatus.OK);
            rb.setMessage("Tag with id '" + tagId + "' has been successfully modified.");
            return new ResponseEntity<>(rb, HttpStatus.OK);
        } catch (ItemDoNotExistsException e1) {
            return customException(rb, e1, HttpStatus.NOT_FOUND);
        } catch (TagAlreadyExistsException | IncorrectValueException | ExceededMaximumNumberOfCharactersException e2) {
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
