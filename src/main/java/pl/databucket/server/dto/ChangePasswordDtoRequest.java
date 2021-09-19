package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordDtoRequest {

    private String username;
    private String password;
    private String newPassword;

}
