package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class SignUpDtoRequest {

    @NotEmpty
    @Size(min = Constants.NAME_MIN, max = Constants.USERNAME_MAX)
    private String username;
    private String email;
    private String password;
    private String url;
    private String recaptchaToken;
    /** Public projects the person asks for; emailed to the SUPER admins once the account is confirmed. */
    private List<Integer> projectsIds;
    /** Optional message for the SUPER admins. */
    private String message;

}
