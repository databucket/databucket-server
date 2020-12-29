package pl.databucket.controller;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.database.ResultField;
import pl.databucket.exception.*;
import pl.databucket.service.DataClassService;
import pl.databucket.response.BaseResponse;
import pl.databucket.response.DataClassResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/class")
@RestController
public class DataClassController {

    private final CustomExceptionFormatter customExceptionFormatter;
    private final DataClassService databucketService;

    @Autowired
    public DataClassController(DataClassService databucketService) {
        this.databucketService = databucketService;
        this.customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(DataClassController.class));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> createClass(@RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
        DataClassResponse response = new DataClassResponse();

        try {
            String className = FieldValidator.validateClassName(body, true);
            String description = FieldValidator.validateDescription(body, false);

            int classId = databucketService.createClass(userName, className, description);
            response.setClassId(classId);
            response.setMessage("The class '" + className + "' has been successfully created.");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ClassAlreadyExistsException | ExceededMaximumNumberOfCharactersException | EmptyInputValueException e) {
            return customExceptionFormatter.customException(response, e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @DeleteMapping(value = "/{classId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> deleteClass(@PathVariable("classId") Integer classId, @RequestParam String userName) {
        DataClassResponse response = new DataClassResponse();
        try {
            databucketService.deleteClass(classId, userName);
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemAlreadyUsedException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (ItemDoNotExistsException e2) {
            return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_FOUND);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @SuppressWarnings("unchecked")
    @GetMapping(value = {"", "/{classId}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> getClasses(
            @PathVariable Optional<Integer> classId,
            @RequestParam(required = false) Optional<Integer> page,
            @RequestParam(required = false) Optional<Integer> limit,
            @RequestParam(required = false) Optional<String> sort,
            @RequestParam(required = false) Optional<String> filter) {
        DataClassResponse response = new DataClassResponse();

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

            Map<ResultField, Object> result = databucketService.getClasses(classId, page, limit, sort, urlConditions);

            long total = (long) result.get(ResultField.TOTAL);
            response.setTotal(total);

            if (page.isPresent() && limit.isPresent()) {
                response.setTotalPages((int) Math.ceil(total / (float) limit.get()));
            }

            response.setClasses((List<Map<String, Object>>) result.get(ResultField.DATA));

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IncorrectValueException e) {
            return customExceptionFormatter.customException(response, e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @PutMapping(value = "/{classId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> modifyClass(@PathVariable("classId") Integer classId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {

        DataClassResponse response = new DataClassResponse();

        try {
            databucketService.modifyClass(userName, classId, body);
            response.setMessage("Class '" + classId + "' has been successfully modified.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemDoNotExistsException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_FOUND);
        } catch (ClassAlreadyExistsException e2) {
            return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

}
