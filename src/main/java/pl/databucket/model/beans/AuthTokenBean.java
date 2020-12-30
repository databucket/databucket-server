package pl.databucket.model.beans;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthTokenBean {

    private String token;
    private boolean changePassword;

}
