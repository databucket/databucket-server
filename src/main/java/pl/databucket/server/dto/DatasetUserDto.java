package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * A user of a dataset, e.g. the demo users that make the history of the data. Users are global: the import creates
 * the ones that don't exist yet, with the password and the role of the dataset, and gives every one of them access
 * to the project (an existing user keeps their password and role).
 */
@Getter
@Setter
public class DatasetUserDto {

    private String username;
    // in plain text: only for demo or training accounts, never for real ones
    private String password;
    // ADMIN, MEMBER, ROBOT or AGENT (not SUPER)
    private String role;
    private String description;

}
