package pl.databucket.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenProvider implements Serializable {

    public static final String AUTHORITIES_KEY = "roles";
    public static final String PROJECT_ID = "p-id";
    public static final String CONTENT = "content";

    @Value("${jwt.secret}")
    private String singingKey;

    @Value("${jwt.expire.hours:24}")
    private long expireHours;

    private final JwtEncoder jwtEncoder;

    public String generateToken(Authentication authentication, Integer projectId) {
        final List<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(authority -> {
                if (authentication instanceof JwtAuthenticationToken) {
                    return authority.replace("ROLE_", "");
                }
                return authority;
            })
            .toList();
        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
            .subject(authentication.getName())
            .claim(AUTHORITIES_KEY, String.join(" ", authorities))
            .claim(PROJECT_ID, projectId)
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plus(expireHours, ChronoUnit.HOURS))
            .build();
        JwsHeader jwsHeader = JwsHeader.with(() -> "HS256").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claimsSet)).getTokenValue();
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
            .setSigningKey(singingKey)
            .parseClaimsJws(token)
            .getBody();
    }

    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public String packToJwts(String content) {
        return Jwts.builder()
            .claim(CONTENT, content)
            .signWith(SignatureAlgorithm.HS256, singingKey)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + 48 * 60 * 60 * 1000))
            .compact();
    }

    /**
     * @aguelfi I think we can do without io.jsonwebtoken and use Spring's Nimbus
     */
    public String unpackFromJwts(String jwts) {
        return Jwts.parser()
            .setSigningKey(singingKey)
            .parseClaimsJws(jwts)
            .getBody().get(CONTENT).toString();
    }

}
