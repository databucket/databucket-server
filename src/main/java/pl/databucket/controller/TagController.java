package pl.databucket.controller;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.database.*;
import pl.databucket.exception.*;
import pl.databucket.service.TagService;
import pl.databucket.response.BaseResponse;
import pl.databucket.response.TagResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/tag")
@RestController
public class TagController {

    private final CustomExceptionFormatter customExceptionFormatter;
    private final TagService service;

    public TagController(TagService service) {
        this.service = service;
        this.customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(TagController.class));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> createTag(@RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {

        TagResponse response = new TagResponse();

        try {
            Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
            String bucketName = FieldValidator.validateBucketName(body, false);
            String tagName = FieldValidator.validateTagName(body, true);
            String description = FieldValidator.validateDescription(body, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);

            Integer tagId = service.createTag(userName, tagName, bucketId, bucketName, description, classId);
            response.setTagId(tagId);
            response.setMessage("The new tag has been successfully created.");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ItemDoNotExistsException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_FOUND);
        } catch (EmptyInputValueException | ExceededMaximumNumberOfCharactersException | IncorrectValueException | TagAlreadyExistsException e2) {
            return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @DeleteMapping(value = "/{tagId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> deleteTags(@PathVariable Integer tagId, @RequestParam String userName) {

        TagResponse response = new TagResponse();

        try {
            service.deleteTag(userName, tagId);
            response.setMessage("The tag has been removed.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemAlreadyUsedException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (ItemDoNotExistsException e2) {
            return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @GetMapping(value = {
            "",
            "/{tagId}",
            "/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> getTags(
            @PathVariable Optional<String> bucketName,
            @PathVariable Optional<Integer> tagId,
            @RequestParam(required = false) Optional<Integer> page,
            @RequestParam(required = false) Optional<Integer> limit,
            @RequestParam(required = false) Optional<String> sort,
            @RequestParam(required = false) Optional<String> filter) {

        TagResponse response = new TagResponse();

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

            Map<ResultField, Object> result = service.getTags(bucketName, tagId, page, limit, sort, urlConditions);

            long total = (long) result.get(ResultField.TOTAL);
            response.setTotal(total);

            if (page.isPresent() && limit.isPresent()) {
                response.setTotalPages((int) Math.ceil(total / (float) limit.get()));
            }

            response.setTags((List<Map<String, Object>>) result.get(ResultField.DATA));

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemDoNotExistsException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_FOUND);
        } catch (IncorrectValueException e2) {
            return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @PutMapping(value = "/{tagId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> modifyTag(@PathVariable("tagId") Integer tagId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {

        TagResponse response = new TagResponse();

        try {
            Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
            String tagName = FieldValidator.validateTagName(body, false);
            String description = FieldValidator.validateDescription(body, false);

            service.modifyTag(userName, tagId, tagName, bucketId, classId, description);
            response.setMessage("Tag with id '" + tagId + "' has been successfully modified.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemDoNotExistsException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_FOUND);
        } catch (TagAlreadyExistsException | IncorrectValueException | ExceededMaximumNumberOfCharactersException e2) {
            return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }
}
