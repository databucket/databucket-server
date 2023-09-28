package pl.databucket.server.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtDecoder implements JwtDecoder {

    private final NimbusJwtDecoder decoder;

    public CustomJwtDecoder(@Value("${jwt.secret}") String singingKey) {
        SecretKey secretKey = new SecretKeySpec(singingKey.getBytes(), "HmacSHA256");
        this.decoder = NimbusJwtDecoder.withSecretKey(secretKey)
            .build();
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        return decoder.decode(token);
    }
}
