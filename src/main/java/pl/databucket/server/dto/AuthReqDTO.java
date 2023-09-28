package pl.databucket.server.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class AuthReqDTO {

    @ApiModelProperty(position = 1, required = true, example = "username")
    @NotEmpty
    private String username;

    @ApiModelProperty(position = 2, required = true, example = "example password")
    @NotEmpty
    private String password;

    @ApiModelProperty(position = 3, example = "1")
    private Integer projectId;

}
