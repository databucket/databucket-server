package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Exchange of a robot's client token from the SSO provider for a Databucket token")
public class SsoTokenExchangeReqDTO {

    @Schema(description = "Access token the robot got from the SSO provider with the client credentials grant",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @Schema(description = "Project ID; can be left out when the robot is assigned to one project", example = "1")
    private Integer projectId;
}
