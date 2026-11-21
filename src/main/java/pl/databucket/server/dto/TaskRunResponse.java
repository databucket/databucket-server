package pl.databucket.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @param count     the rows changed, removed, reserved or with the history cleared; in a preview the rows the task
 *                  would run on
 * @param available reserve: the free rows still matching the rules (in a preview: before reserving)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "The result of running a task")
public record TaskRunResponse(
        @Schema(description = "Task id", example = "3") long taskId,
        @Schema(description = "Task name", example = "mark as used") String task,
        @Schema(description = "The action of the task: modify, remove, clear history or reserve", example = "modify") String action,
        @Schema(description = "True: nothing was changed, count is the number of rows the task would run on") boolean preview,
        @Schema(description = "Rows changed, removed, reserved or with the history cleared; in a preview the rows the task would run on", example = "12") long count,
        @Schema(description = "Reserve: free rows still matching the rules") Long available,
        @Schema(example = "Modified 12 data row(s)") String message) {
}
