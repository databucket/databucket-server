package pl.databucket.server.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.service.data.SearchRules;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DataRemoveDTO implements SearchRules {

    @ApiModelProperty(hidden = true, example = "[{\"left_source\": \"field\", \"left_value\": \"data_id\", \"operator\": \"<\", \"right_source\": \"const\", \"right_value\": 100},\n" +
            "{\"left_source\": \"property\",\t\"left_value\": \"$.name\",\t\"operator\": \"like\",\t\"right_source\": \"const\", \"right_value\": \"Jo%\"}]")
    private List<Map<String, Object>> conditions; // Old filtering method

    @ApiModelProperty(hidden = true)
    private Map<String, Object> logic; // New rules defined in frontend (property is saved as prop.$*group*itemName)

    @ApiModelProperty(position = 1, example = "[[\"$.firstName\",\"like\",\"S%a\"],[\"id\", \">\",  10],[\"tagId\",\">\", 0],{\"or\": [[\"owner\", \"!=\", null],[\"createdBy\", \"=\", \"@currentUser\"]]}]")
    private List<Object> rules;

}
