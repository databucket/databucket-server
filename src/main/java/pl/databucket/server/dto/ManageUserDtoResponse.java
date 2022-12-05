package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class ManageUserDtoResponse {

    private long id;
    private String username;
    private String description;
    private String email;
    private Boolean enabled;
    private Date expirationDate;
    private Set<Short> rolesIds;
    private Set<Integer> projectsIds;

    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
}
