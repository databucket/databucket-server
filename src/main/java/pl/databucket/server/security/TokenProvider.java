package pl.databucket.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class TokenProvider implements Serializable {

    public static final String AUTHORITIES_KEY = "a-key";
    public static final String PROJECT_ID = "p-id";
    public static final String CONTENT = "content";

    @Value("${jwt.secret}")
    private String singingKey;

    @Value("${jwt.expire.hours}")
    private long expireHours;

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
            .setSigningKey(singingKey)
            .parseClaimsJws(token)
            .getBody();
    }

    public Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    public String generateToken(Authentication authentication, Integer projectId) {
        final String authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(","));
        return Jwts.builder()
            .setSubject(authentication.getName())
            .claim(AUTHORITIES_KEY, authorities)
            .claim(PROJECT_ID, projectId)
            .signWith(SignatureAlgorithm.HS256, singingKey)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + expireHours * 60 * 60 * 1000))
            .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String packToJwts(String content) {
        return Jwts.builder()
            .claim(CONTENT, content)
            .signWith(SignatureAlgorithm.HS256, singingKey)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + 48 * 60 * 60 * 1000))
            .compact();
    }

    public String unpackFromJwts(String jwts) {
        return Jwts.parser()
            .setSigningKey(singingKey)
            .parseClaimsJws(jwts)
            .getBody().get(CONTENT).toString();
    }

    public Jwt parseJwt(String token) {
        return Jwts.parser().setSigningKey(singingKey).parse(token);
    }

    public CustomUserDetails parseToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);

            final Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            return CustomUserDetails.builder()
                .username(claims.getSubject())
                .authorities(authorities)
                .build();

        } catch (JwtException | ClassCastException e) {
            return null;
        }
    }

    public UsernamePasswordAuthenticationToken getAuthentication(final String token) {
        final Claims claims = Jwts.parser()
            .setSigningKey(singingKey)
            .parseClaimsJws(token)
            .getBody();

        List<String> roles = List.of(claims.get(AUTHORITIES_KEY).toString().split(","));
        final Collection<SimpleGrantedAuthority> authorities =
            Stream.of(roles, List.of(claims.get(PROJECT_ID).toString()))
                .flatMap(Collection::stream)
                .map(SimpleGrantedAuthority::new)
                .toList();

        CustomUserDetails principal = CustomUserDetails.builder()
            .username(claims.getSubject())
            .authorities(authorities)
            .projectId(claims.get(PROJECT_ID, Integer.class))
            .build();
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }
//    UsernamePasswordAuthenticationToken getAuthentication(final String token,
//        final CustomUserDetails customUserDetails) {
//
//        final Claims claims = Jwts.parser()
//            .setSigningKey(singingKey)
//            .parseClaimsJws(token)
//            .getBody();
//
//        customUserDetails.setProjectId((Integer) claims.get(PROJECT_ID));
//
//        final Collection<? extends GrantedAuthority> authorities =
//            Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
//                .map(SimpleGrantedAuthority::new)
//                .toList();
//
//        return new UsernamePasswordAuthenticationToken(customUserDetails, "", authorities);
//    }

}
