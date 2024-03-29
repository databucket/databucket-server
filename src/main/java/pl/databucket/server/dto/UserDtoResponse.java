package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class UserDtoResponse {

    private long id;
    private String username;
    private Boolean enabled;
    private Set<Short> rolesIds;
    private Set<Short> teamsIds;
    private Date expirationDate;

    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
}
