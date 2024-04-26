package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class AccessTreeViewDto {
    private Long id;
    private String name;
    private String description;
    private Set<Long> classesIds;
    private Set<Long> bucketsIds;
    private long columnsId;
    private Long filterId;
    private Short[] featuresIds;
}
