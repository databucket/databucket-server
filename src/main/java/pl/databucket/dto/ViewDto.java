package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.configuration.Constants;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class ViewDto {
    private Long id;
    @Size(min = Constants.NAME_MIN, max = Constants.NAME_MAX)
    private String name;
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;
    private boolean enabledDetails;
    private boolean enabledCreation;
    private boolean enabledModifying;
    private boolean enabledRemoval;
    private boolean enabledImport;
    private boolean enabledExport;
    private boolean enabledHistory;
    private boolean enabledTasks;
    private boolean enabledReservation;
    private Set<Long> classesIds;
    private Set<Long> bucketsIds;
    private Set<Long> usersIds;
    private Set<Short> teamsIds;
    private long columnsId;
    private Long filterId;
    private Short roleId;

    private String createdBy;
    private Date createdDate;
    private String lastModifiedBy;
    private Date lastModifiedDate;
}
