package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class ViewDto {

    // asc(<field>) or desc(<field>), as the sort parameter of the API
    public static final String SORT = "(asc|desc)\\((id|tagId|reserved|owner|createdAt|createdBy|modifiedAt|modifiedBy|\\$\\.[^()'\"]+)\\)";

    private Long id;
    @Size(min = Constants.NAME_MIN, max = Constants.NAME_MAX)
    private String name;
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;
    private Set<Long> classesIds;
    private Set<Long> bucketsIds;
    private Set<Long> usersIds;
    private Set<Short> teamsIds;
    private long columnsId;
    // the sort the data table starts with, as the sort parameter of the API, e.g. desc(modifiedAt) or asc($.lastName); null = by id
    @Pattern(regexp = SORT, message = "must be asc(<field>) or desc(<field>), the field one of id, tagId, reserved, owner, createdAt, createdBy, modifiedAt, modifiedBy or a property $.<path>")
    @Size(max = 250)
    private String sort;
    private Long filterId;
    private Short roleId;
    private Short[] featuresIds;

    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
}
