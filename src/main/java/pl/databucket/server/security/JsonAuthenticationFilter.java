package pl.databucket.server.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

/**
 * Allows for a POST JSON sigin in instead of the x-www-form-urlencoded version when calling POST /login-form
 */
@Component
public class JsonAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper mapper;
    /**
     * We can't call request.getReader() twice, so we save the result of the first call in a ThreadLocal var as not to
     * mix different requests.
     */
    private static final ThreadLocal<Map<String, String>> ongoingAuth = new ThreadLocal<>();

    public JsonAuthenticationFilter(ObjectMapper mapper, AuthenticationManager authenticationManager,
        AuthenticationSuccessHandler successHandler) {
        super(authenticationManager);
        this.setAuthenticationSuccessHandler(successHandler);
        this.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/api/public/sign-in",
            "POST"));
        this.mapper = mapper;
    }

    @Override
    protected String obtainUsername(HttpServletRequest request) {
        try {
            Map<String, String> map = mapper.readValue(request.getReader(), new TypeReference<>() {
            });
            ongoingAuth.set(map);
            return map.get(SPRING_SECURITY_FORM_USERNAME_KEY);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String obtainPassword(HttpServletRequest request) {
        return ongoingAuth.get().get(SPRING_SECURITY_FORM_PASSWORD_KEY);
    }
}
