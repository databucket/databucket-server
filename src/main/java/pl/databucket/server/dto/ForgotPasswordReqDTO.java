package pl.databucket.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ForgotPasswordReqDTO {
    private String email;
    private String url;
}
