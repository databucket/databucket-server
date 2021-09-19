package pl.databucket.server.controller;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.TaskDto;
import pl.databucket.server.entity.Task;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.service.TaskService;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/tasks")
@RestController
public class TaskController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(TaskController.class);

    @Autowired
    private TaskService taskService;

    @Autowired
    private ModelMapper modelMapper;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createTask(@Valid @RequestBody TaskDto taskDto) {
        try {
            Task task = taskService.createTask(taskDto);
            modelMapper.map(task, taskDto);
            return new ResponseEntity<>(taskDto, HttpStatus.CREATED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping
    public ResponseEntity<?> getTasks() {
        try {
            List<Task> tasks = taskService.getTasks();
            List<TaskDto> tasksDto = tasks.stream().map(item -> modelMapper.map(item, TaskDto.class)).collect(Collectors.toList());
            return new ResponseEntity<>(tasksDto, HttpStatus.OK);
        } catch (Exception ee) {
            return exceptionFormatter.defaultException(ee);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ResponseEntity<?> modifyTask(@Valid @RequestBody TaskDto taskDto) {
        try {
            Task task = taskService.modifyTask(taskDto);
            modelMapper.map(task, taskDto);
            return new ResponseEntity<>(taskDto, HttpStatus.OK);
        } catch (ModifyByNullEntityIdException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable long taskId) {
        try {
            taskService.deleteTask(taskId);
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
