package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserDtoResponse {

    private long id;
    private String name;
    private boolean changePassword;
    private Boolean enabled;
    private Set<Long> roles;

}
