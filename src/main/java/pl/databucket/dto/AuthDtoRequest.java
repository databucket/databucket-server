package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthDtoRequest {

    private String name;
    private String password;
    private Integer projectId;

}
