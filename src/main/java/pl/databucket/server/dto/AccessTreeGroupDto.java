package pl.databucket.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class AccessTreeGroupDto {
    private Long id;
    private String name;
    private String shortName;
    private Set<Long> bucketsIds;
}
