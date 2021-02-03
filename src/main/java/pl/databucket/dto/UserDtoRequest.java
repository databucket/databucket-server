package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.configuration.Constants;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class UserDtoRequest {

    @NotEmpty
    @Size(min = Constants.NAME_MIN, max = Constants.NAME_MAX)
    private String username;
    private String password;
    private boolean enabled = false;
    private Date expirationDate;
    private Set<Short> rolesIds;
    private Set<Integer> projectsIds;
    private Set<Long> groupsIds;
    private Set<Long> bucketsIds;

}
