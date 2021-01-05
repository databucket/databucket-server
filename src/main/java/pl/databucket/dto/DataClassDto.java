package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class DataClassDto {

    private Long id;

    @NotBlank(message = "Class name must not be blank!")
    @Size(min=3, max=50)
    private String name;
    private String description;
}
