package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TaskDto {

    private Long id;

    @NotBlank(message = "Task name must not be blank!")
    @Size(min=3, max=50)
    private String name;
    private String description;
    private List<Long> bucketId;
    private List<Long> classId;
    private Map<String, Object> configuration;
}
