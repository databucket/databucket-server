package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class ManageUserDtoRequest {

    @NotEmpty
    @Size(min = Constants.NAME_MIN, max = Constants.NAME_MAX)
    private String username;
    private String description;
    private String email;
    private String password;
    private boolean enabled = false;
    private Date expirationDate;
    private Set<Short> rolesIds;
    private Set<Integer> projectsIds;

}
