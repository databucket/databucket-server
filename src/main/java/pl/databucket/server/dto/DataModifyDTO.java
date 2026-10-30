package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.service.data.SearchRules;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Schema(description = "Data transfer object for modifying data")
public class DataModifyDTO implements SearchRules {

    @Schema(description = "Tag ID", example = "1")
    private Long tagId;

    @Schema(description = "True reserves the rows for the signed-in user, false releases them", example = "false")
    private Boolean reserved;

    @Schema(description = "Replaces all the properties. To change only some of them, use propertiesToSet and propertiesToRemove instead.",
            example = "{\"name\": \"John\", \"age\": 34}")
    private Map<String, Object> properties;

    @Schema(description = "Properties to set, by JSON path; the other properties stay unchanged",
            example = "{\"$.address.city\": \"Paris\", \"$.age\": 35}")
    private Map<String, Object> propertiesToSet;

    @Schema(description = "Properties to remove, by JSON path", example = "[\"$.phone\"]")
    private List<String> propertiesToRemove;

    @Schema(
            hidden = true,
            description = "Old filtering method - conditions",
            example = "[{\"left_source\": \"field\", \"left_value\": \"data_id\", \"operator\": \"<\", \"right_source\": \"const\", \"right_value\": 100}, {\"left_source\": \"property\", \"left_value\": \"$.name\", \"operator\": \"like\", \"right_source\": \"const\", \"right_value\": \"Jo%\"}]"
    )
    private List<Map<String, Object>> conditions;

    @Schema(
            hidden = true,
            description = "New rules defined in frontend (property is saved as prop.$*group*itemName)"
    )
    private Map<String, Object> logic;

    @Schema(
            description = "Rules selecting the rows to modify (ignored when modifying by ids)",
            example = "[[\"$.firstName\",\"like\",\"S%a\"],[\"id\", \">\", 10],[\"tagId\",\">\", 0],{\"or\": [[\"owner\", \"!=\", null],[\"createdBy\", \"=\", \"@currentUser\"]]}]"
    )
    private List<Object> rules;

}
