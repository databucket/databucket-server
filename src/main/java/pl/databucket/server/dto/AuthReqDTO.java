package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication request data transfer object")
public class AuthReqDTO {

    @Schema(description = "Username", example = "username", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "Password", example = "example password", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Project ID", example = "1")
    private Integer projectId;

}
