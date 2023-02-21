package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

@Getter
@Setter
public class AuthProjectDTO {

    @Schema(example = "1")
    private int id;

    @Schema(example = "DEMO")
    private String name;

    @Schema(example = "Demo project")
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;

    @Schema(example = "true")
    private boolean enabled;

    @Schema(example = "false")
    private boolean expired;

}
