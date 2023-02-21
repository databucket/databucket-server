package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthReqDTO {

    @Schema(requiredMode = RequiredMode.REQUIRED, example = "username")
    private String username;

    @Schema(requiredMode = RequiredMode.REQUIRED, example = "example password")
    private String password;

    @Schema(example = "1")
    private Integer projectId;

}
