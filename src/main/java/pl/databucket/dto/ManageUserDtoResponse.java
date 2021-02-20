package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class ManageUserDtoResponse {

    private long id;
    private String username;
    private Boolean enabled;
    private Date expirationDate;
    private Set<Short> rolesIds;
    private Set<Integer> projectsIds;

    private String createdBy;
    private Date createdDate;
    private String lastModifiedBy;
    private Date lastModifiedDate;
}
