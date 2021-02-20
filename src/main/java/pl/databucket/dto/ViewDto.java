package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.configuration.Constants;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.Date;

@Getter
@Setter
public class ViewDto {
    private Long id;
    @Size(min = Constants.NAME_MIN, max = Constants.NAME_MAX)
    private String name;
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;
    private Long dataClassId;
    private Long bucketId;
    @NotEmpty
    private long columnsId;
    private Long filterId;
    private boolean privateItem;

    private String createdBy;
    private Date createdDate;
    private String lastModifiedBy;
    private Date lastModifiedDate;
}
