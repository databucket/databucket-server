package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UserDtoRequest {

    private String username;
    private Set<Short> teamsIds;
}
