package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class UserDtoResponse {

    private String name;
    private Boolean enabled;
    private Set<Short> rolesIds;
    private Set<Integer> projectsIds;
    private Set<Long> groupsIds;
    private Set<Long> bucketsIds;

    private String createdBy;
    private Date createdDate;
    private String lastModifiedBy;
    private Date lastModifiedDate;
}
