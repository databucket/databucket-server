package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;
import javax.validation.constraints.Size;
import java.util.Date;

@Getter
@Setter
public class TemplateDataDto {

    private Integer id;
    @Size(min = 1, max = Constants.NAME_MAX)
    private String name;
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;
    private int templateId;

    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
}
