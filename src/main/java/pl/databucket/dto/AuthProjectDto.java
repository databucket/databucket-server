package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.configuration.Constants;

import javax.validation.constraints.Max;
import javax.validation.constraints.Size;

@Getter
@Setter
public class AuthProjectDto {

    private int id;
    private String name;
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;

}
