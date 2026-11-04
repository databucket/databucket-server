package pl.databucket.server.dto;

import java.util.List;

/** What the registration form after an SSO login shows: the email, a suggested user name and the public projects. */
public record SsoRegistrationInfoDTO(String email, String username, List<PublicProjectDto> projects) {
}
