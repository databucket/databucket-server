package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

import jakarta.validation.constraints.Size;

@Getter
@Setter
@Schema(description = "Authentication project data transfer object")
public class AuthProjectDTO {

    @Schema(description = "Project ID", example = "1")
    private int id;

    @Schema(description = "Project name", example = "DEMO")
    private String name;

    @Schema(description = "Project description", example = "Demo project")
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;

    @Schema(description = "Project enabled flag", example = "true")
    private boolean enabled;

    @Schema(description = "Project expired flag", example = "false")
    private boolean expired;

}
