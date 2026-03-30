package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.service.data.SearchRules;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Schema(description = "Data transfer object for retrieving data with optional filtering and column selection")
public class DataGetDTO implements SearchRules {

    @Schema(
            description = "Custom columns to retrieve",
            example = "[{\"field\": \"id\",\"title\": \"id\"},{\"field\": \"$.firstName\",\"title\": \"name\"}]"
    )
    private List<CustomColumnDto> columns;

    @Schema(
            hidden = true,
            description = "Old filtering method - conditions",
            example = "[{\"left_source\": \"field\", \"left_value\": \"data_id\", \"operator\": \"<\", \"right_source\": \"const\", \"right_value\": 100}, {\"left_source\": \"property\", \"left_value\": \"$.name\", \"operator\": \"like\", \"right_source\": \"const\", \"right_value\": \"Jo%\"}]"
    )
    private List<Map<String, Object>> conditions;

    @Schema(
            description = "Search rules for filtering data",
            example = "[[\"$.firstName\",\"like\",\"S%a\"],[\"id\", \">\", 10],[\"tagId\",\">\", 0],{\"or\": [[\"owner\", \"!=\", null],[\"createdBy\", \"=\", \"@currentUser\"]]}]"
    )
    private List<Object> rules;

    @Schema(
            hidden = true,
            description = "New rules defined in frontend (property is saved as prop.$*group*itemName)"
    )
    private Map<String, Object> logic;
}
