package pl.databucket.server.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.databucket.server.service.UserService;

public class JwtOauthTokenFilter extends OncePerRequestFilter {

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";

    @Autowired
    private UserService userService;

    @Autowired
    private TokenProvider jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String header = req.getHeader(HEADER_AUTHORIZATION);
        String username = null;
        String authToken = null;
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            authToken = header.trim().replaceFirst("\ufeff", "").replace(TOKEN_PREFIX, "");    // Fixing: Unexpected character ('�' (code 65533 / 0xfffd))
            try {
                username = jwtTokenUtil.getUsernameFromToken(authToken);
            } catch (io.jsonwebtoken.MalformedJwtException e) {
                logger.error("The token is broken", e);
            } catch (IllegalArgumentException e) {
                logger.error("An error occurred during getting user name from token", e);
            } catch (ExpiredJwtException e) {
//                logger.info("The token is expired and not valid anymore.");
            } catch (SignatureException e) {
//                logger.info("Authentication Failed. User name, password or token not valid.");
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            CustomUserDetails customUserDetails = (CustomUserDetails) userService.loadUserByUsername(username);

            if (jwtTokenUtil.validateToken(authToken, customUserDetails)) {
                UsernamePasswordAuthenticationToken authentication = jwtTokenUtil.getAuthentication(authToken, customUserDetails);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        chain.doFilter(req, res);
    }
}
