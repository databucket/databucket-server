package pl.databucket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthDto {

    private String token;
    private Boolean changePassword;
    private String message;
    private List<AuthProjectDto> projects;

}
