package pl.databucket.server.security;

import io.jsonwebtoken.MalformedJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

//@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    private final TokenProvider jwtUtil;

    @Override
    public boolean supports(Class<?> authentication) {
        return (BearerTokenAuthenticationToken.class.isAssignableFrom(authentication));
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails,
        UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
    }

    @Override
    protected UserDetails retrieveUser(String username, UsernamePasswordAuthenticationToken authentication)
        throws AuthenticationException {
//        BearerTokenAuthenticationToken jwtAuthenticationToken = (BearerTokenAuthenticationToken) authentication;
//        String token = jwtAuthenticationToken.getToken().getTokenValue();

        CustomUserDetails parsedUser = jwtUtil.parseToken("token");

        if (parsedUser == null) {
            throw new MalformedJwtException("JWT token is not valid");
        }
        return parsedUser;
    }

}

