package pl.databucket.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthRespDTO {

    @Schema(example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdXBlciIsImEta2V5IjoiUk9MRV9ST0JPVCxST0xFX01FTUJFUixST0xFX1NVUEVSLFJPTEVfQURNSU.rsHfwqgquz1e-YYC9UgyYSDWrEdpPQSnMV_XbrpHT6I")
    private String token;
    private AuthProjectDTO project;
    @Schema(accessMode = AccessMode.READ_ONLY)
    private Boolean changePassword;
    @Schema(accessMode = AccessMode.READ_ONLY)
    private String message;
    @Schema(accessMode = AccessMode.READ_ONLY)
    private List<AuthProjectDTO> projects;
    @Schema(example = "[\"ROBOT\", \"MEMBER\"]")
    private List<String> roles;

}
