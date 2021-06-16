package pl.databucket.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class DataCreateDTO {

    @ApiModelProperty(position = 1, example = "1")
    private Long tagId;

    @ApiModelProperty(position = 2, example = "false")
    private Boolean reserved;

    @ApiModelProperty(position = 3, example = "{\"name\": \"John\", \"age\": 34}")
    private Map<String, Object> properties;

}
