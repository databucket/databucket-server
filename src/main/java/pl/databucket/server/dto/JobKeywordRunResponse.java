package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** The result of running (or previewing) the jobs of a keyword: one entry per job, by id. */
public record JobKeywordRunResponse(
        @Schema(description = "The keyword as given") String keyword,
        @Schema(description = "true: nothing was changed, the counts are the rows the jobs would run on") boolean preview,
        @Schema(description = "The jobs that ran without an error") int ok,
        @Schema(description = "The jobs that failed") int errors,
        List<Job> jobs) {

    public record Job(
            @Schema(description = "The job id") long id,
            @Schema(description = "The job name") String name,
            @Schema(description = "The task name") String task,
            @Schema(description = "The bucket name") String bucket,
            @Schema(description = "OK or ERROR") String status,
            @Schema(description = "The number of data rows") Long count,
            @Schema(description = "What the task did, or why it failed") String message) {
    }
}
