package pl.databucket.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.UriComponentsBuilder;
import pl.databucket.server.dto.AuthRespDTO;
import pl.databucket.server.dto.ManageUserDtoRequest;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.SomeItemsNotFoundException;
import pl.databucket.server.service.ManageUserService;
import pl.databucket.server.service.UserService;

@Log4j2
public class OAuth2AuthSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final ManageUserService manageUserService;
    private final UserService userService;
    private final AuthResponseBuilder authResponseBuilder;
    private static final ObjectMapper mapper = new JsonMapper();

    public OAuth2AuthSuccessHandler(AuthResponseBuilder authResponseBuilder,
        ManageUserService manageUserService,
        UserService userService) {
        super.setDefaultTargetUrl("/login-callback");
        this.manageUserService = manageUserService;
        this.userService = userService;
        this.authResponseBuilder = authResponseBuilder;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
        HttpServletResponse response, Authentication authentication) throws IOException {
        String projectid = request.getParameter("projectid");
        AuthRespDTO authResponse = buildAuthResponse(authentication, projectid);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        responseWrapper.setContentType("application/json");
        responseWrapper.setCharacterEncoding("UTF-8");
        responseWrapper.getWriter().write(mapper.writeValueAsString(authResponse));
        String targetUri = UriComponentsBuilder.fromUriString("/login-callback")
            .queryParam("token", authResponse.getToken())
            .build().toUriString();
        getRedirectStrategy().sendRedirect(request, response, targetUri);
    }


    protected AuthRespDTO buildAuthResponse(Authentication authentication, String projectid) {
        if (authentication instanceof OAuth2AuthenticationToken token) {
            UserDetails userDetails = userService.loadUserByUsername(token.getPrincipal().getName());
            CustomUserDetails user = Optional.ofNullable(userDetails)
                .map(CustomUserDetails.class::cast)
                .orElseGet(() -> createNewOauthUser(token));
            return authResponseBuilder.buildAuthResponse(
                new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities()),
                projectid);
        } else if (authentication instanceof UsernamePasswordAuthenticationToken token) {
            return authResponseBuilder.buildAuthResponse(token, projectid);
        }
        return AuthRespDTO.builder().message("Wrong authentication").build();
    }

    private CustomUserDetails createNewOauthUser(OAuth2AuthenticationToken auth) {
        try {
            ManageUserDtoRequest newUserRequest = ManageUserDtoRequest.builder()
                .username(auth.getPrincipal().getName())
                .email(auth.getPrincipal().getAttributes().getOrDefault("email", "").toString())
                .enabled(true)
                .build();
            User user = manageUserService.createUser(newUserRequest);
            return CustomUserDetails.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(getAuthority(user))
                .enabled(user.getEnabled())
                .expired(user.isExpired())
                .superUser(user.isSuperUser())
                .changePassword(user.isChangePassword())
                .projects(user.getProjects())
                .build();
        } catch (ItemAlreadyExistsException | SomeItemsNotFoundException e) {
            throw new DuplicateKeyException("user already exists");
        }
    }

    private Set<SimpleGrantedAuthority> getAuthority(User user) {
        return user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority(role.getName()))
            .collect(Collectors.toSet());
    }

}
