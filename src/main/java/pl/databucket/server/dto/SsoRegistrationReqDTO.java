package pl.databucket.server.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** Creating an account after an SSO login without one; the ticket comes from the /login/sso?register= redirect. */
@Getter
@Setter
@NoArgsConstructor
public class SsoRegistrationReqDTO {

    private String ticket;

    private String username;

    /** Public projects the person asks for; emailed to the SUPER admins. */
    private List<Integer> projectsIds;

    /** Optional message for the SUPER admins. */
    private String message;
}
