package pl.databucket.server.security;

import javax.servlet.ServletException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;

//@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint, Serializable {

    private final AuthenticationEntryPoint delegate;

    public JwtAuthenticationEntryPoint(AuthenticationEntryPoint delegate) {
        this.delegate = delegate;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

//        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        delegate.commence(request, response, authException);
    }
}
