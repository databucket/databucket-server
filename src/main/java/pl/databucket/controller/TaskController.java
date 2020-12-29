package pl.databucket.controller;

import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.database.C;
import pl.databucket.database.COL;
import pl.databucket.database.Condition;
import pl.databucket.database.FieldValidator;
import pl.databucket.exception.*;
import pl.databucket.service.TaskService;
import pl.databucket.response.BaseResponse;
import pl.databucket.response.TaskResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/task")
@RestController
public class TaskController {

    private final CustomExceptionFormatter customExceptionFormatter;
    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
        this.customExceptionFormatter = new CustomExceptionFormatter(LoggerFactory.getLogger(TaskController.class));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> createTask(@RequestParam String userName, @RequestBody Map<String, Object> body) {
        TaskResponse response = new TaskResponse();

        try {
            String taskName = FieldValidator.validateTaskName(body, true);
            Map<String, Object> configuration = FieldValidator.validateTaskConfiguration(body, true);
            String description = FieldValidator.validateDescription(body, false);
            Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);

            int taskId = service.createTask(taskName, bucketId, classId, userName, description, configuration);
            response.setTaskId(taskId);
            response.setMessage("The new task has been successfully created.");
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ItemDoNotExistsException e) {
            return customExceptionFormatter.customException(response, e, HttpStatus.NOT_FOUND);
        } catch (EmptyInputValueException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @DeleteMapping(value = "/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> deleteTask(@PathVariable Integer taskId, @RequestParam String userName) {
        TaskResponse response = new TaskResponse();
        try {
            service.deleteTask(userName, taskId);
            response.setMessage("The task has been removed.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemAlreadyUsedException e1) {
            return customExceptionFormatter.customException(response, e1, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @GetMapping(value = {
            "",
            "/{taskId}",
            "/buckets/{bucketName}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> getTasks(
            @PathVariable Optional<String> bucketName,
            @PathVariable Optional<Integer> taskId,
            @RequestParam(required = false) Optional<Integer> page,
            @RequestParam(required = false) Optional<Integer> limit,
            @RequestParam(required = false) Optional<String> sort,
            @RequestParam(required = false) Optional<String> filter) {

        TaskResponse response = new TaskResponse();
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

            Map<String, Object> result = service.getTasks(bucketName, taskId, page, limit, sort, urlConditions);

            long total = (long) result.get(C.TOTAL);
            response.setTotal(total);

            if (page.isPresent() && limit.isPresent()) {
                response.setTotalPages((int) Math.ceil(total / (float) limit.get()));
            }

            response.setTasks((List<Map<String, Object>>) result.get(C.TASKS));

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ItemDoNotExistsException e) {
            return customExceptionFormatter.customException(response, e, HttpStatus.NOT_FOUND);
        } catch (IncorrectValueException e2) {
            return customExceptionFormatter.customException(response, e2, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception ee) {
            return customExceptionFormatter.defaultException(response, ee);
        }
    }

    @PutMapping(value = "/{taskId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BaseResponse> modifyTask(@PathVariable("taskId") Integer taskId, @RequestParam String userName, @RequestBody LinkedHashMap<String, Object> body) {
        TaskResponse response = new TaskResponse();
        try {
            Integer bucketId = FieldValidator.validateNullableId(body, COL.BUCKET_ID, false);
            Integer classId = FieldValidator.validateNullableId(body, COL.CLASS_ID, false);
            String taskName = FieldValidator.validateTaskName(body, false);
            String description = FieldValidator.validateDescription(body, false);
            LinkedHashMap<String, Object> configuration = FieldValidator.validateTaskConfiguration(body, false);

            service.modifyTask(userName, taskId, taskName, bucketId, classId, description, configuration);
            response.setMessage("Task with id '" + taskId + "' has been successfully modified.");
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
