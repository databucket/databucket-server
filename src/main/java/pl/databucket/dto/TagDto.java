package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class TagDto {

    private Long id;

    @NotBlank(message = "Tag name must not be blank!")
    @Size(min=3, max=50)
    private String name;

    @Max(250)
    private String description;
    private List<Long> bucketId;
    private List<Long> classId;
}
