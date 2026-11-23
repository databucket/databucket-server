package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pl.databucket.server.configuration.OpenApiConfiguration;
import pl.databucket.server.dto.JobKeywordRunDTO;
import pl.databucket.server.dto.JobKeywordRunResponse;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.service.TaskScheduleService;

/** Running the jobs of a keyword (Settings > Jobs) from outside, e.g. a CI pipeline; managing jobs: TaskScheduleController. */
@Tag(name = "Jobs", description = "Running the jobs saved in the project settings (Settings → Jobs) by their keyword, "
        + "e.g. 'DEV cleaning'. Requires the ROBOT role or higher; each job runs its task as the caller, so the caller "
        + "needs access to the jobs' buckets.")
@SecurityRequirement(name = OpenApiConfiguration.BEARER)
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/task-schedules")
@RestController
public class JobKeywordController {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(JobKeywordController.class);

    private final TaskScheduleService scheduleService;

    public JobKeywordController(TaskScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PreAuthorize("hasRole('ROBOT')")
    @Operation(summary = "Run the jobs of a keyword", description = "Runs all the jobs with the keyword (compared "
            + "ignoring case), one after another by id, each with its task, bucket and rules, as the caller - also the "
            + "disabled ones (disabling stops only the cron). A failed job doesn't stop the others; every run is in "
            + "the history of its job. With preview = true nothing is changed and 'count' is the number of rows each "
            + "job would run on.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - the result of each job (also when some failed)",
                    content = @Content(schema = @Schema(implementation = JobKeywordRunResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - no keyword"),
            @ApiResponse(responseCode = "404", description = "Not found - no job has the keyword; the message lists the keywords"),
            @ApiResponse(responseCode = "409", description = "Conflict - the jobs of the keyword are running"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping(value = "/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> runKeyword(
            @Parameter(description = "True: change nothing, only count the rows each job would run on", example = "false")
            @RequestParam(required = false, defaultValue = "false") boolean preview,
            @RequestBody JobKeywordRunDTO request) {
        try {
            return new ResponseEntity<>(scheduleService.runKeyword(request != null ? request.getKeyword() : null, preview),
                    HttpStatus.OK);
        } catch (TaskScheduleService.InvalidScheduleException e) {
            return exceptionFormatter.customException(e, HttpStatus.BAD_REQUEST);
        } catch (TaskScheduleService.KeywordNotFoundException e) {
            return exceptionFormatter.customException(e, HttpStatus.NOT_FOUND);
        } catch (TaskScheduleService.KeywordRunningException e) {
            return exceptionFormatter.customException(e, HttpStatus.CONFLICT);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
