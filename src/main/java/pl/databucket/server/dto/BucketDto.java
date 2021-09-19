package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class BucketDto {

    private Long id;
    @Size(min=Constants.NAME_MIN, max=Constants.NAME_MAX)
    private String name;
    private String iconName;
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;
    private Long classId;
    private Set<Long> groupsIds;
    private Set<Long> usersIds;
    private Set<Short> teamsIds;
    private boolean history;
    private boolean protectedData;
    private Short roleId;

    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;

}
