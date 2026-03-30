package pl.databucket.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Authentication response data transfer object")
public class AuthRespDTO {

    @Schema(
            description = "JWT authentication token",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdXBlciIsImEta2V5IjoiUk9MRV9ST0JPVCxST0xFX01FTUJFUixST0xFX1NVUEVSLFJPTEVfQURNSU.rsHfwqgquz1e-YYC9UgyYSDWrEdpPQSnMV_XbrpHT6I"
    )
    private String token;

    @Schema(hidden = true, description = "Flag indicating if password change is required")
    private Boolean changePassword;

    @Schema(hidden = true, description = "Response message")
    private String message;

    @Schema(hidden = true, description = "List of available projects")
    private List<AuthProjectDTO> projects;

    @Schema(description = "Selected project details")
    private AuthProjectDTO project;

    @Schema(description = "User roles", example = "[\"ROBOT\", \"MEMBER\"]")
    private List<String> roles;

}
