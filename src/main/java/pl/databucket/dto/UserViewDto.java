package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class UserViewDto {
    private Long id;
    private String name;
    private String description;
    private Set<Long> classesIds;
    private Set<Long> bucketsIds;
    private long columnsId;
    private Long filterId;
    private Short[] featuresIds;
}
