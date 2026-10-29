package pl.databucket.server.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TokenProvider implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String AUTHORITIES_KEY = "a-key";
    public static final String PROJECT_ID = "p-id";
    public static final String CONTENT = "content";
    public static final String SSO_USER = "sso-user";

    @Value("${jwt.secret}")
    private String signingKey;

    @Value("${jwt.expire.hours}")
    private long expireHours;


    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8));
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
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
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    public String generateToken(Authentication authentication, Integer projectId) {
        final String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .claim(PROJECT_ID, projectId)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expireHours * 60 * 60 * 1000))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String packToJwts(String content) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .claim(CONTENT, content)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 48 * 60 * 60 * 1000)) // 48 godzin
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Short-lived code handed to the browser after an SSO login and exchanged for a token.
     * It has no subject, so the JWT filter never accepts it as a token, and no content claim,
     * so it can't be used as a confirmation link either.
     */
    public String generateSsoCode(String username, long ttlSeconds) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claim(SSO_USER, username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlSeconds * 1000))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * @return the username the code was issued for
     * @throws IllegalArgumentException when the code is invalid, expired or not an SSO code
     */
    public String getUsernameFromSsoCode(String code) {
        try {
            String username = getAllClaimsFromToken(code).get(SSO_USER, String.class);
            if (username == null)
                throw new IllegalArgumentException("Not an SSO code");
            return username;
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid SSO code", e);
        }
    }

    public String unpackFromJwts(String jwts) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(jwts)
                    .getPayload()
                    .get(CONTENT, String.class);
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    UsernamePasswordAuthenticationToken getAuthentication(final String token, final CustomUserDetails customUserDetails) {
        try {
            final Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            customUserDetails.setProjectId(claims.get(PROJECT_ID, Integer.class));

            final Collection<? extends GrantedAuthority> authorities =
                    Arrays.stream(claims.get(AUTHORITIES_KEY, String.class).split(","))
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

            return new UsernamePasswordAuthenticationToken(customUserDetails, "", authorities);
        } catch (JwtException e) {
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }
}
