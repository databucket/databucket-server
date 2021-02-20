package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class UserDtoRequest {

    private String username;
    private Set<Long> groupsIds;
    private Set<Long> bucketsIds;
    private Set<Long> viewsIds;
}
