package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class DataEnumDto {
    private Integer id;
    @Size(min = Constants.NAME_MIN, max = Constants.NAME_MAX)
    private String name;
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;
    private boolean iconsEnabled;
    private List<DataEnumItemDto> items;

    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
}
