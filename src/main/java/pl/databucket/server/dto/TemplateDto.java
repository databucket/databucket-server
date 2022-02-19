package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
public class TemplateDto {
    private Integer id;
    private String name;
    private String description;
    private Set<Integer> projectsIds;
    private TemplateConfDto configuration;

    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
}
