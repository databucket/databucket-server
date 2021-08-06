package pl.databucket.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.service.data.SearchRules;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DataModifyDTO implements SearchRules {

    @ApiModelProperty(position = 1, example = "1")
    private Long tagId;

    @ApiModelProperty(position = 2, example = "false")
    private Boolean reserved;

    @ApiModelProperty(position = 3, example = "{\"name\": \"John\", \"age\": 34}")
    private Map<String, Object> properties;

    @ApiModelProperty(hidden = true)
    private Map<String, Object> propertiesToSet;

    @ApiModelProperty(hidden = true)
    private List<String> propertiesToRemove;

    @ApiModelProperty(position = 4, example = "[{\"left_source\": \"field\", \"left_value\": \"data_id\", \"operator\": \"<\", \"right_source\": \"const\", \"right_value\": 100},\n" +
            "{\"left_source\": \"property\",\t\"left_value\": \"$.name\",\t\"operator\": \"like\",\t\"right_source\": \"const\", \"right_value\": \"Jo%\"}]")
    private List<Map<String, Object>> conditions; // Old filtering method

    @ApiModelProperty(hidden = true)
    private Map<String, Object> logic; // New rules defined in frontend (property is saved as prop.$*group*itemName)

    @ApiModelProperty(hidden = true)
    private List<Object> rules; // New rules defined in code (property is saved as $.group.itemName)

}
