package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class UserDtoRequest {

    private String name;
    private String password;
    private Boolean enabled;
    private Set<Short> rolesIds;
    private Set<Integer> projectsIds;
    private Set<Long> groupsIds;
    private Set<Long> bucketsIds;

}
