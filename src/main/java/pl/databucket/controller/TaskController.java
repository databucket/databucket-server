package pl.databucket.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.databucket.dto.TaskDto;
import pl.databucket.exception.ExceptionFormatter;
import pl.databucket.service.TaskService;
import pl.databucket.specification.TaskSpecification;

import javax.validation.Valid;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/tasks")
@RestController
public class TaskController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(TaskController.class);

    @Autowired
    private TaskService taskService;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createTask(@Valid @RequestBody TaskDto taskDto) {
        try {
            return new ResponseEntity<>(taskService.createTask(taskDto), HttpStatus.CREATED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getTasks(TaskSpecification specification, Pageable pageable) {
        try {
            return new ResponseEntity<>(taskService.getTasks(specification, pageable), HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PutMapping
    public ResponseEntity<?> modifyTask(@Valid @RequestBody TaskDto taskDto) {
        try {
            return new ResponseEntity<>(taskService.modifyTask(taskDto), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @DeleteMapping(value = "/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable long taskId) {
        try {
            taskService.deleteTask(taskId);
            return new ResponseEntity<>(null, HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
