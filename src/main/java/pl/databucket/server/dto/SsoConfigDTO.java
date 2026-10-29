package pl.databucket.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Single sign-on options for the login page")
public class SsoConfigDTO {

    @Schema(description = "Whether the SSO button is shown")
    private boolean enabled;

    @Schema(description = "Button style: microsoft, google, okta, keycloak or generic", example = "microsoft")
    private String provider;

    @Schema(description = "Button label suffix (\"Sign in with ...\")", example = "Microsoft")
    private String name;
}
