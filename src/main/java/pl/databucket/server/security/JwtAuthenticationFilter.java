package pl.databucket.server.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Log4j2
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String TOKEN_PREFIX = "Bearer ";

    private final TokenProvider jwtTokenUtil;

    @Override
    public void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain)
        throws IOException, ServletException {
        String authHeader = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (!ObjectUtils.isEmpty(authHeader)) {
            UsernamePasswordAuthenticationToken authentication = jwtTokenUtil.getAuthentication(
                authHeader.substring(TOKEN_PREFIX.length()));
            boolean tokenValid = jwtTokenUtil.validateToken(authHeader.substring(TOKEN_PREFIX.length()),
                CustomUserDetails.builder()
                    .username(authentication.getName())
                    .build());
            if (tokenValid) {
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(req, res);
    }

//    @Override
//    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
//        String header = req.getHeader(HEADER_AUTHORIZATION);
//        String username = null;
//        String authToken = null;
//        if (header != null && header.startsWith(TOKEN_PREFIX)) {
//            authToken = header.trim().replaceFirst("\ufeff", "").replace(TOKEN_PREFIX, "");    // Fixing: Unexpected character ('�' (code 65533 / 0xfffd))
//            try {
//                username = jwtTokenUtil.getUsernameFromToken(authToken);
//            } catch (io.jsonwebtoken.MalformedJwtException e) {
//                logger.error("The token is broken", e);
//            } catch (IllegalArgumentException e) {
//                logger.error("An error occurred during getting user name from token", e);
//            } catch (ExpiredJwtException e) {
////                logger.info("The token is expired and not valid anymore.");
//            } catch (SignatureException e) {
////                logger.info("Authentication Failed. User name, password or token not valid.");
//            }
//        }
//
//        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//
//            CustomUserDetails customUserDetails = (CustomUserDetails) userService.loadUserByUsername(username);
//
//            if (jwtTokenUtil.validateToken(authToken, customUserDetails)) {
//                UsernamePasswordAuthenticationToken authentication = jwtTokenUtil.getAuthentication(authToken, customUserDetails);
//                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
//                SecurityContextHolder.getContext().setAuthentication(authentication);
//            }
//        }
//
//        chain.doFilter(req, res);
//    }
}
