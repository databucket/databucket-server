package pl.databucket.web.tasks;

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
@RequestMapping("/api/tasks")
@RestController
public class TasksController {

    private static final Logger logger = LoggerFactory.getLogger(TasksController.class);

    private final DatabucketService databucketService;

    public TasksController(DatabucketService databucketService) {
        this.databucketService = databucketService;
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> createTask(@RequestParam String userName, @RequestBody Map<String, Object> body) {
        ResponseBody rb = new ResponseBody();

        try {
            String taskName = FieldValidator.validateTaskName(body, true);
            Map<String, Object> configuration = FieldValidator.validateTaskConfiguration(body, true);
            String description = FieldValidator.validateDescription(body, false);
            Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);

            int taskId = databucketService.createTask(taskName, bucketId, classId, userName, description, configuration);
            rb.setStatus(ResponseStatus.OK);
            rb.setTaskId(taskId);
            rb.setMessage("The new task has been successfully created.");
            return new ResponseEntity<>(rb, HttpStatus.CREATED);
        } catch (ItemDoNotExistsException e) {
            return customException(rb, e, HttpStatus.NOT_FOUND);
        } catch (EmptyInputValueException e1) {
            return customException(rb, e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return defaultException(rb, ee);
        }
    }

    @DeleteMapping(value = "/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> deleteTask(@PathVariable Integer taskId, @RequestParam String userName) {
        ResponseBody rb = new ResponseBody();
        try {
            databucketService.deleteTask(userName, taskId);
            rb.setStatus(ResponseStatus.OK);
            rb.setMessage("The task has been removed.");
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
            "/{taskId}",
            "/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> getTasks(
            @PathVariable Optional<String> bucketName,
            @PathVariable Optional<Integer> taskId,
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

            Map<String, Object> result = databucketService.getTasks(bucketName, taskId, page, limit, sort, urlConditions);

            long total = (long) result.get(C.TOTAL);
            rb.setTotal(total);

            if (page.isPresent() && limit.isPresent()) {
                rb.setTotalPages((int) Math.ceil(total / (float) limit.get()));
            }

            rb.setTasks((List<Map<String, Object>>) result.get(C.TASKS));

            return new ResponseEntity<>(rb, HttpStatus.OK);
        } catch (ItemDoNotExistsException e) {
            return customException(rb, e, HttpStatus.NOT_FOUND);
        } catch (IncorrectValueException e2) {
            return customException(rb, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return defaultException(rb, ee);
        }
    }

    @PutMapping(value = "/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseBody> modifyTask(@PathVariable("taskId") Integer taskId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
        ResponseBody rb = new ResponseBody();
        try {
            Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
            String taskName = FieldValidator.validateTaskName(body, false);
            String description = FieldValidator.validateDescription(body, false);
            LinkedHashMap<String, Object> configuration = FieldValidator.validateTaskConfiguration(body, false);

            databucketService.modifyTask(userName, taskId, taskName, bucketId, classId, description, configuration);
            rb.setStatus(ResponseStatus.OK);
            rb.setMessage("Task with id '" + taskId + "' has been successfully modified.");
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
