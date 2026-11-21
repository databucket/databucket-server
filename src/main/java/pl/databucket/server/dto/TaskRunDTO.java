package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.service.data.SearchRules;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Schema(description = "Rules narrowing the rows a task runs on; combined with 'and' with the filter of the task")
public class TaskRunDTO implements SearchRules {

    @Schema(hidden = true, description = "The oldest filter format, translated like the rules; use rules")
    private List<Map<String, Object>> conditions;

    @Schema(hidden = true, description = "Rules as JsonLogic (property as prop.$*group*itemName)")
    private Map<String, Object> logic;

    @Schema(
            description = "Rules selecting the rows; required when the task has no filter",
            example = "[[\"$.status\", \"==\", \"used\"], [\"modifiedAt\", \"<\", \"2026-11-01T00:00:00.000Z\"]]"
    )
    private List<Object> rules;

}
