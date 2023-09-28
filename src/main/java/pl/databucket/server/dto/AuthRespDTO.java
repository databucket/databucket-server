package pl.databucket.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Builder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthRespDTO {

    @ApiModelProperty(position = 1, example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzdXBlciIsImEta2V5IjoiUk9MRV9ST0JPVCxST0xFX01FTUJFUixST0xFX1NVUEVSLFJPTEVfQURNSU.rsHfwqgquz1e-YYC9UgyYSDWrEdpPQSnMV_XbrpHT6I")
    private String token;

    @ApiModelProperty(hidden = true)
    private Boolean changePassword;

    @ApiModelProperty(hidden = true)
    private String message;

    @ApiModelProperty(hidden = true)
    private String username;

    @ApiModelProperty(hidden = true)
    private List<AuthProjectDTO> projects;

    @ApiModelProperty(position = 2)
    private AuthProjectDTO project;

    @ApiModelProperty(position = 3, example = "[\"ROBOT\", \"MEMBER\"]")
    private List<String> roles;

}
