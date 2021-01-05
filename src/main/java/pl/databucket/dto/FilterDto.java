package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class FilterDto {

    private Long id;

    @NotBlank(message = "Filter name must not be blank!")
    @Size(min=3, max=50)
    private String name;
    private String description;
    private Long bucketId;
    private Long classId;
    private List<CriteriaDto> creteria;
}
