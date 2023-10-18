package pl.databucket.server.security;

import io.jsonwebtoken.impl.TextCodec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtDecoder implements JwtDecoder {

    private final NimbusJwtDecoder decoder;

    public CustomJwtDecoder(@Value("${jwt.secret}") String singingKey) {
        var encodedKey = TextCodec.BASE64.encode(singingKey).getBytes(StandardCharsets.UTF_8);
        var paddedKey = encodedKey.length < 128 ? Arrays.copyOf(encodedKey, 128) : encodedKey;
        SecretKey secretKey = new SecretKeySpec(paddedKey, "HS512");
        this.decoder = NimbusJwtDecoder
            .withSecretKey(secretKey)
            .macAlgorithm(MacAlgorithm.HS512)
            .build();
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        return decoder.decode(token);
    }
}
