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
import java.util.List;
import java.util.UUID;
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
    public static final String SSO_REGISTER = "sso-register";
    static final String REQUESTED_PROJECTS = "req-p";
    static final String REQUESTED_MESSAGE = "req-m";

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
                .id(UUID.randomUUID().toString())
                .claim(SSO_USER, username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlSeconds * 1000))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /** The content of a valid SSO code: the user, the code's own id (jti) and when it expires. */
    public record SsoCode(String username, String id, Date expiresAt) {
    }

    /**
     * Checks the signature and expiry of an SSO code; whether it was already used is checked by {@link SsoCodeService}.
     *
     * @throws IllegalArgumentException when the code is invalid, expired or not an SSO code
     */
    public SsoCode parseSsoCode(String code) {
        Claims claims;
        try {
            claims = getAllClaimsFromToken(code);
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid SSO code", e);
        }
        String username = claims.get(SSO_USER, String.class);
        if (username == null || claims.getId() == null)
            throw new IllegalArgumentException("Not an SSO code");
        return new SsoCode(username, claims.getId(), claims.getExpiration());
    }

    /** What a person asked for while signing up: public projects and a message for the SUPER admins. */
    public record SignUpRequest(List<Integer> projectIds, String message) {
    }

    /**
     * Sign-up confirmation link: like {@link #packToJwts(String)} with the email, plus the projects and the message,
     * which are emailed to the SUPER admins once the account is confirmed (they are stored nowhere else).
     */
    public String packSignUpToJwts(String email, List<Integer> projectIds, String message) {
        long now = System.currentTimeMillis();
        JwtBuilder builder = Jwts.builder()
                .claim(CONTENT, email)
                .issuedAt(new Date(now))
                .expiration(new Date(now + 48 * 60 * 60 * 1000));
        if (projectIds != null && !projectIds.isEmpty())
            builder.claim(REQUESTED_PROJECTS, projectIds);
        if (message != null)
            builder.claim(REQUESTED_MESSAGE, message);
        return builder.signWith(getSigningKey(), Jwts.SIG.HS256).compact();
    }

    /** The projects and the message of a sign-up confirmation link; empty for links without them. */
    public SignUpRequest readSignUpRequest(String jwts) {
        Claims claims = getAllClaimsFromToken(jwts);
        List<?> ids = claims.get(REQUESTED_PROJECTS, List.class);
        List<Integer> projectIds = ids == null ? List.of() : ids.stream()
                .filter(Number.class::isInstance)
                .map(id -> ((Number) id).intValue())
                .toList();
        return new SignUpRequest(projectIds, claims.get(REQUESTED_MESSAGE, String.class));
    }

    /** A registration ticket: an unknown person signed in at the SSO provider and may create an account. */
    public record SsoRegistrationTicket(String email, String id, Date expiresAt) {
    }

    /**
     * Single-use ticket handed to the browser when someone without an account signs in through SSO and may register.
     * Like the SSO code it has no subject and no content claim, so it is neither a token nor a confirmation link.
     */
    public String generateSsoRegistrationTicket(String email, long ttlSeconds) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claim(SSO_REGISTER, email)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlSeconds * 1000))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * @throws IllegalArgumentException when the ticket is invalid, expired or not a registration ticket
     */
    public SsoRegistrationTicket parseSsoRegistrationTicket(String ticket) {
        Claims claims;
        try {
            claims = getAllClaimsFromToken(ticket);
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid registration ticket", e);
        }
        String email = claims.get(SSO_REGISTER, String.class);
        if (email == null || claims.getId() == null)
            throw new IllegalArgumentException("Not a registration ticket");
        return new SsoRegistrationTicket(email, claims.getId(), claims.getExpiration());
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
