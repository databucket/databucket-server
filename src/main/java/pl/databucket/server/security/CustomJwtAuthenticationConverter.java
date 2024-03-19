package pl.databucket.server.security;

import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import pl.databucket.server.service.UserService;

@Component
@RequiredArgsConstructor
public class CustomJwtAuthenticationConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private final UserService userService;
    private final TokenProvider jwtTokenUtil;

    @Override
    public JwtAuthenticationToken convert(Jwt source) {
        String username = source.getSubject();
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            CustomUserDetails customUserDetails = (CustomUserDetails) userService.loadUserByUsername(username);
            if (jwtTokenUtil.validateToken(source.getTokenValue(), customUserDetails)) {
                return jwtTokenUtil.getAuthentication(source.getTokenValue(), customUserDetails);
            }
        }
        return null;
    }
}
