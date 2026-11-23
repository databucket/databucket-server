package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.dto.TaskScheduleDto;
import pl.databucket.server.exception.BucketNotFoundException;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.InvalidRuleException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.UnknownColumnException;
import pl.databucket.server.service.ScheduleCron;
import pl.databucket.server.service.TaskAction;
import pl.databucket.server.service.TaskRunService;
import pl.databucket.server.service.TaskScheduleService;

/**
 * Settings > Jobs (the web application); running a task from outside: TaskRunController, the jobs of a keyword:
 * JobKeywordController.
 */
@Hidden
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/task-schedules")
@RestController
public class TaskScheduleController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(TaskScheduleController.class);

    private final TaskScheduleService scheduleService;

    public TaskScheduleController(TaskScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getSchedules() {
        try {
            return new ResponseEntity<>(scheduleService.getSchedules(), HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createSchedule(@Valid @RequestBody TaskScheduleDto dto) {
        try {
            return new ResponseEntity<>(scheduleService.createSchedule(dto), HttpStatus.CREATED);
        } catch (Exception e) {
            return error(e);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> modifySchedule(@Valid @RequestBody TaskScheduleDto dto) {
        try {
            return new ResponseEntity<>(scheduleService.modifySchedule(dto), HttpStatus.OK);
        } catch (Exception e) {
            return error(e);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> deleteSchedule(@PathVariable long id) {
        try {
            scheduleService.deleteSchedule(id);
            return new ResponseEntity<>(null, HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return error(e);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/{id}/runs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getRuns(@PathVariable long id) {
        try {
            return new ResponseEntity<>(scheduleService.getRuns(id), HttpStatus.OK);
        } catch (Exception e) {
            return error(e);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> runNow(@PathVariable long id) {
        try {
            return new ResponseEntity<>(scheduleService.runNow(id), HttpStatus.OK);
        } catch (Exception e) {
            return error(e);
        }
    }

    // the checks of saving a schedule: their messages say what to change
    private ResponseEntity<?> error(Exception e) {
        if (e instanceof ItemNotFoundException)
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        if (e instanceof TaskRunService.ForbiddenTaskException)
            return exceptionFormatter.customException(e, HttpStatus.FORBIDDEN);
        if (e instanceof TaskScheduleService.InvalidScheduleException || e instanceof ScheduleCron.InvalidCronException
                || e instanceof TaskAction.InvalidTaskException || e instanceof TaskRunService.TaskNotFoundException
                || e instanceof BucketNotFoundException || e instanceof InvalidRuleException
                || e instanceof UnknownColumnException)
            return exceptionFormatter.customException(e, HttpStatus.BAD_REQUEST);
        return exceptionFormatter.defaultException(e);
    }
}
