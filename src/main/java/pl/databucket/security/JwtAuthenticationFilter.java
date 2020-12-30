package pl.databucket.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.databucket.service.UserService;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static pl.databucket.security.Constants.HEADER_AUTHORIZATION;
import static pl.databucket.security.Constants.TOKEN_PREFIX;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

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
            authToken = header.replace(TOKEN_PREFIX,"");
            try {
                username = jwtTokenUtil.getUsernameFromToken(authToken);
            } catch (IllegalArgumentException e) {
                logger.error("An error occurred during getting user name from token", e);
            } catch (ExpiredJwtException e) {
                logger.warn("The token is expired and not valid anymore.");
            } catch(SignatureException e){
                logger.warn("Authentication Failed. User name, password or token not valid.");
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userService.loadUserByUsername(username);

            if (jwtTokenUtil.validateToken(authToken, userDetails)) {
                UsernamePasswordAuthenticationToken authentication = jwtTokenUtil.getAuthentication(authToken, SecurityContextHolder.getContext().getAuthentication(), userDetails);
                //UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, Arrays.asList(new SimpleGrantedAuthority("ROLE_ADMIN")));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                logger.info("Authenticated user " + username + ", setting security context.");
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        chain.doFilter(req, res);
    }
}