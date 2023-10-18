package pl.databucket.server.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.jsonwebtoken.impl.TextCodec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomJwtEncoder implements JwtEncoder {

    private final NimbusJwtEncoder delegate;

    public CustomJwtEncoder(@Value("${jwt.secret}") String singingKey) {
        var encodedKey = TextCodec.BASE64.encode(singingKey).getBytes(StandardCharsets.UTF_8);
        var paddedKey = encodedKey.length < 128 ? Arrays.copyOf(encodedKey, 128) : encodedKey;
        SecretKey secretKey = new SecretKeySpec(paddedKey, "HS512");
        JWKSource<SecurityContext> immutableSecret = new ImmutableSecret<>(secretKey);
        delegate = new NimbusJwtEncoder(immutableSecret);
    }


    @Override
    public Jwt encode(JwtEncoderParameters parameters) throws JwtEncodingException {
        return delegate.encode(parameters);
    }
}
