package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class BucketDto {

    private Long id;

    @NotBlank(message = "Bucket name must not be blank!")
    @Size(min=3, max=50)
    private String name;

    private String iconName;
    private String description;
    private Long dataClassId;
    private boolean history = false;
    private List<Integer> users;
}
