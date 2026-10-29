package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Exchange of the code received after an SSO login for a token")
public class SsoTokenReqDTO {

    @Schema(description = "Code from the /login/sso redirect", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "Project ID", example = "1")
    private Integer projectId;
}
