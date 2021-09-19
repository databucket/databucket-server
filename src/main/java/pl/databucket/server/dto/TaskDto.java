package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class TaskDto {

    private Long id;
    @Size(min = Constants.NAME_MIN, max = Constants.NAME_MAX)
    private String name;
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;
    private Long classId;
    private Long filterId;
    private Set<Long> classesIds;
    private Set<Long> bucketsIds;
    private TaskConfigDto configuration;

    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;

}
