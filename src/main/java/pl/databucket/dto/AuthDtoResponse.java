package pl.databucket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthDtoResponse {

    private String token;
    private Boolean changePassword;
    private String message;
    private List<AuthProjectDto> projects;
    private AuthProjectDto project;
    private List<String> roles;

}
