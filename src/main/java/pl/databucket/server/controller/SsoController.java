package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import pl.databucket.server.configuration.SsoProperties;
import pl.databucket.server.dto.SsoConfigDTO;
import pl.databucket.server.dto.SsoTokenReqDTO;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.repository.UserRepository;
import pl.databucket.server.security.AuthResponseBuilder;
import pl.databucket.server.security.SsoClientRegistrationRepository;
import pl.databucket.server.security.TokenProvider;
import pl.databucket.server.service.UserService;

import java.net.URI;

@Hidden // used only by the web application
@Tag(name = "PUBLIC", description = "Public endpoints for authentication and user registration")
@RestController
@RequestMapping("/api/public/sso")
public class SsoController {

    private static final String SSO_UNAVAILABLE = "sso_unavailable";

    @Autowired
    private SsoProperties ssoProperties;

    @Autowired
    private ObjectProvider<SsoClientRegistrationRepository> clientRegistrationRepository;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthResponseBuilder authResponseBuilder;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(SsoController.class);

    @Operation(summary = "SSO options", description = "Whether single sign-on is enabled and how its button looks.")
    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public SsoConfigDTO getConfig() {
        if (!ssoProperties.isEnabled())
            return new SsoConfigDTO(false, null, null);
        return new SsoConfigDTO(true, ssoProperties.getProvider(), ssoProperties.getName());
    }

    @Operation(summary = "Start SSO login", description = "Redirects to the identity provider, or back to the login page when SSO is not available.")
    @GetMapping("/login")
    public ResponseEntity<Void> login(HttpServletRequest request) {
        SsoClientRegistrationRepository repository = clientRegistrationRepository.getIfAvailable();
        String target = repository != null && repository.getRegistration() != null
                ? "/oauth2/authorization/" + SsoClientRegistrationRepository.REGISTRATION_ID
                : "/login?sso_error=" + SSO_UNAVAILABLE;
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(request.getContextPath() + target).toString())
                .build();
    }

    @Operation(summary = "End SSO session", description = "Redirects to the identity provider's logout when sso.provider-logout is on, otherwise straight to the login page.")
    @GetMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String loginPage = ServletUriComponentsBuilder.fromContextPath(request).path("/login").toUriString();
        String target = loginPage;

        SsoClientRegistrationRepository repository = clientRegistrationRepository.getIfAvailable();
        if (ssoProperties.isEnabled() && ssoProperties.isProviderLogout() && repository != null) {
            ClientRegistration registration = repository.getRegistration();
            Object endSessionEndpoint = registration != null
                    ? registration.getProviderDetails().getConfigurationMetadata().get("end_session_endpoint")
                    : null;
            // e.g. Google has no end_session_endpoint
            if (endSessionEndpoint != null)
                target = UriComponentsBuilder.fromUriString(endSessionEndpoint.toString())
                        .queryParam("client_id", registration.getClientId())
                        .queryParam("post_logout_redirect_uri", loginPage)
                        .encode()
                        .toUriString();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, target)
                .build();
    }

    @Operation(
            summary = "Exchange SSO code",
            description = "Exchanges the code from the SSO login for the same response as /api/public/signin. "
                    + "When the user has several projects the response lists them; send the code again with the chosen projectId."
    )
    @PostMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> exchangeCode(@RequestBody SsoTokenReqDTO ssoTokenReqDTO) {
        if (!ssoProperties.isEnabled())
            return exceptionFormatter.customPublicException("Single sign-on is not enabled", HttpStatus.NOT_FOUND);

        String username;
        try {
            username = tokenProvider.getUsernameFromSsoCode(ssoTokenReqDTO.getCode());
        } catch (IllegalArgumentException e) {
            AuditLog.log("auth.sso-signin-failed", "reason={}", e.getMessage());
            return exceptionFormatter.customPublicException("The sign-in has expired. Please sign in again.", HttpStatus.UNAUTHORIZED);
        }

        // the user may have been removed or disabled since the code was issued
        User user = userRepository.findWithProjectsAndRolesByUsername(username);
        if (user == null || !user.getEnabled()) {
            AuditLog.log("auth.sso-signin-failed", "user={} reason={}", username, user == null ? "unknown user" : "disabled");
            return exceptionFormatter.customPublicException("User is disabled", HttpStatus.UNAUTHORIZED);
        }

        try {
            UserDetails userDetails = userService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            return authResponseBuilder.build(user, authentication, ssoTokenReqDTO.getProjectId(), false, "auth.sso-signin");
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
