package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.service.data.SearchRules;

@Getter
@Setter
public class DataGetDTO implements SearchRules {

    @Schema(example = "[{\"field\": \"id\",\"title\": \"id\"},{\"field\": \"$.firstName\",\"title\": \"name\"}]")
    private List<CustomColumnDto> columns;

    @Schema(accessMode = AccessMode.READ_ONLY, example =
        "[{\"left_source\": \"field\", \"left_value\": \"data_id\", \"operator\": \"<\", \"right_source\": \"const\", \"right_value\": 100},\n"
            +
            "{\"left_source\": \"property\",\t\"left_value\": \"$.name\",\t\"operator\": \"like\",\t\"right_source\": \"const\", \"right_value\": \"Jo%\"}]")
    private List<Map<String, Object>> conditions; // Old filtering method

    @Schema(example = "[[\"$.firstName\",\"like\",\"S%a\"],[\"id\", \">\",  10],[\"tagId\",\">\", 0],{\"or\": [[\"owner\", \"!=\", null],[\"createdBy\", \"=\", \"@currentUser\"]]}]")
    private List<Object> rules;

    @Schema(accessMode = AccessMode.READ_ONLY)
    private Map<String, Object> logic; // New rules defined in frontend (property is saved as prop.$*group*itemName)
}
