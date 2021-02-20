package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.configuration.Constants;

import javax.validation.constraints.Max;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class GroupDto {
    private Long id;
    @Size(min = Constants.NAME_MIN, max = Constants.NAME_MAX)
    private String name;
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;
    private Set<Long> bucketsIds;
    private Set<Long> usersIds;
    private boolean privateItem;

    private String createdBy;
    private Date createdDate;
    private String lastModifiedBy;
    private Date lastModifiedDate;
}
