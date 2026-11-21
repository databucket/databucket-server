package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.configuration.OpenApiConfiguration;
import pl.databucket.server.dto.TaskRunDTO;
import pl.databucket.server.dto.TaskRunResponse;
import pl.databucket.server.exception.BucketNotFoundException;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.InvalidRuleException;
import pl.databucket.server.exception.UnknownColumnException;
import pl.databucket.server.service.TaskAction;
import pl.databucket.server.service.TaskRunService;

import java.util.Objects;

@Tag(name = "Tasks", description = "Running the tasks saved in the project settings (Settings → Tasks) on the data "
        + "of a bucket. Requires the AGENT role or higher and access to the bucket; tasks removing rows or clearing "
        + "the history require ROBOT or higher.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER)
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/bucket/{bucketName}/tasks")
@RestController
public class TaskRunController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(TaskRunController.class);

    private final TaskRunService taskRunService;

    public TaskRunController(TaskRunService taskRunService) {
        this.taskRunService = taskRunService;
    }

    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Run a task", description = "Runs a saved task (modify, remove, clear history or reserve) "
            + "on the data rows matching the filter of the task and the rules of the request, combined with 'and'. "
            + "A task without a filter needs rules. With preview = true nothing is changed and 'count' is the number "
            + "of rows the task would run on (reserve: the rows it would reserve).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - what the task did",
                    content = @Content(schema = @Schema(implementation = TaskRunResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid rules, see https://github.com/databucket/databucket-server/wiki/Rules"),
            @ApiResponse(responseCode = "403", description = "Forbidden - the task removes rows or clears the history, which needs ROBOT or higher"),
            @ApiResponse(responseCode = "404", description = "Not found - the bucket doesn't exist, the user has no access to it, or the task isn't one of the bucket's tasks"),
            @ApiResponse(responseCode = "406", description = "Not acceptable - no filter and no rules, the task changes nothing, the tag doesn't exist, or the bucket keeps no history"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/{taskId}/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> runTask(
            @Parameter(description = "Bucket name, as in the project settings", required = true)
            @PathVariable String bucketName,
            @Parameter(description = "Task id, as in the project settings", required = true)
            @PathVariable long taskId,
            @Parameter(description = "True: change nothing, only count the rows the task would run on", example = "false")
            @RequestParam(required = false, defaultValue = "false") boolean preview,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Rules narrowing the rows; required when the task has no filter")
            @RequestBody(required = false) TaskRunDTO rules) {
        try {
            return new ResponseEntity<>(taskRunService.run(bucketName, String.valueOf(taskId), rules, preview), HttpStatus.OK);
        } catch (BucketNotFoundException | TaskRunService.TaskNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (TaskRunService.ForbiddenTaskException e) {
            return exceptionFormatter.customException(e, HttpStatus.FORBIDDEN);
        } catch (TaskAction.InvalidTaskException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (InvalidRuleException | UnknownColumnException e) {
            return exceptionFormatter.customException(e, HttpStatus.BAD_REQUEST);
        } catch (DataIntegrityViolationException e) {
            if (Objects.requireNonNull(e.getMessage()).contains("is not present in table \"tags\""))
                return exceptionFormatter.customException("The tag set by the task doesn't exist", HttpStatus.NOT_ACCEPTABLE);
            else if (e.getMessage().contains("cannot cast jsonb null"))
                return exceptionFormatter.customException("Failed to operate on an empty property!", HttpStatus.NOT_ACCEPTABLE);
            else
                return exceptionFormatter.customException(e, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
