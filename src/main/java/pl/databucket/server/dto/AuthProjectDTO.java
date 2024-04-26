package pl.databucket.server.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

import javax.validation.constraints.Size;

@Getter
@Setter
public class AuthProjectDTO {

    @ApiModelProperty(position = 1, example = "1")
    private int id;

    @ApiModelProperty(position = 2, example = "DEMO")
    private String name;

    @ApiModelProperty(position = 3, example = "Demo project")
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;

    @ApiModelProperty(position = 4, example = "true")
    private boolean enabled;

    @ApiModelProperty(position = 5, example = "false")
    private boolean expired;

}
