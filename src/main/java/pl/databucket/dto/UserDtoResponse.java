package pl.databucket.dto;

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
    private Set<Long> groupsIds;
    private Set<Long> bucketsIds;
    private Set<Long> viewsIds;
    private Date expirationDate;

    private String createdBy;
    private Date createdDate;
    private String lastModifiedBy;
    private Date lastModifiedDate;
}
