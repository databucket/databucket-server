package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class GroupDto {
    private Long id;
    @Size(min = Constants.NAME_MIN, max = Constants.NAME_MAX)
    private String name;
    private String shortName;
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;
    private Set<Long> bucketsIds;
    private Set<Long> usersIds;
    private Set<Short> teamsIds;
    private Short roleId;

    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
}
