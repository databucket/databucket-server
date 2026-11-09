package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.service.data.SearchRules;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Schema(description = "Search of the history: which data rows (by their current state) and which history entries")
public class DataHistoryGetDTO implements SearchRules {

    @Schema(hidden = true)
    private List<Map<String, Object>> conditions;

    @Schema(
            description = "Rules selecting the data rows by their current state, as in 'Get data rows by rules'. "
                    + "Without them the history of all rows.",
            example = "[[\"$.status\", \"==\", \"active\"]]"
    )
    private List<Object> rules;

    @Schema(description = "The data rows as JsonLogic, instead of 'rules'")
    private Map<String, Object> logic;

    @Schema(
            description = "Rules selecting the history entries. Fields: id (of the entry), dataId, tagId, reserved, "
                    + "modifiedBy, modifiedAt; the properties of the versions can't be searched.",
            example = "[[\"modifiedBy\", \"==\", \"tests-robot\"], [\"modifiedAt\", \">=\", \"2026-11-09T00:00:00.000Z\"]]"
    )
    private List<Object> historyRules;

    @Schema(description = "The history entries as JsonLogic, instead of 'historyRules'")
    private Map<String, Object> historyLogic;
}
