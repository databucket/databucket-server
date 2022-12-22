package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Getter
@Setter
public class SignUpDtoRequest {

    @NotEmpty
    @Size(min = Constants.NAME_MIN, max = Constants.NAME_MAX)
    private String username;
    private String email;
    private String password;
    private String url;
    private String recaptchaToken;

}
