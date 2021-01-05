package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

@Getter
@Setter
public class EventDto {

    private Long id;

    @NotBlank(message = "Event name must not be blank!")
    @Size(min=3, max=50)
    private String name;

    private String description;
    private boolean active;
    private Long bucketId;
    private Long classId;
    private Map<String, Object> schedule;
    private Map<String, Object> tasks;

}
