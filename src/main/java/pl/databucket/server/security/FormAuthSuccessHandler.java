package pl.databucket.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import pl.databucket.server.dto.AuthRespDTO;

@RequiredArgsConstructor
public class FormAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthResponseBuilder authResponseBuilder;
    protected static final ObjectMapper mapper = new JsonMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
        HttpServletResponse response, Authentication authentication) throws IOException {
        String projectid = request.getParameter("projectid");
        AuthRespDTO authResponse = authResponseBuilder.buildAuthResponse(
            (UsernamePasswordAuthenticationToken) authentication, projectid);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(mapper.writeValueAsString(authResponse));
    }

}
