package pl.databucket.server.exception;

import lombok.EqualsAndHashCode;
import lombok.Value;
import pl.databucket.server.dto.AuthRespDTO;

@EqualsAndHashCode(callSuper = true)
@Value
public class AuthForbiddenException extends RuntimeException {


    AuthRespDTO response;

    public AuthForbiddenException(AuthRespDTO response) {
        super(response.getMessage());
        this.response = response;
    }

}
