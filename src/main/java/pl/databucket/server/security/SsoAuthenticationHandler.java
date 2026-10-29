package pl.databucket.server.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import pl.databucket.server.configuration.SsoProperties;
import pl.databucket.server.entity.User;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.repository.UserRepository;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Ends the IdP login: matches the person to an existing Databucket user by email and sends the browser
 * to the login page with a short-lived code (exchanged at /api/public/sso/token) or with an error reason.
 * Nothing of the IdP login is kept on the server.
 */
@Log4j2
@Component
public class SsoAuthenticationHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    // reasons passed to the login page as ?sso_error=
    public static final String NO_EMAIL = "no_email";
    public static final String EMAIL_NOT_VERIFIED = "email_not_verified";
    public static final String UNKNOWN_USER = "unknown_user";
    public static final String USER_DISABLED = "user_disabled";
    public static final String IDP_ERROR = "idp_error";

    @Autowired
    private SsoProperties ssoProperties;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TokenProvider tokenProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        invalidateSession(request);
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        Object claim = principal.getAttribute(ssoProperties.getEmailClaim());
        String email = User.normalizeEmail(claim != null ? claim.toString() : null);

        if (email == null) {
            refuse(request, response, principal.getName(), NO_EMAIL);
            return;
        }
        // providers that verify emails say so; trust an email only if it is not explicitly unverified
        if ("email".equals(ssoProperties.getEmailClaim()) && Boolean.FALSE.equals(principal.getAttribute("email_verified"))) {
            refuse(request, response, email, EMAIL_NOT_VERIFIED);
            return;
        }

        User user = userRepository.findByEmailIgnoreCase(email);
        if (user == null) {
            refuse(request, response, email, UNKNOWN_USER);
            return;
        }
        if (!user.getEnabled()) {
            refuse(request, response, email, USER_DISABLED);
            return;
        }

        AuditLog.log("auth.sso-login", "user={} email={}", user.getUsername(), email);
        String code = tokenProvider.generateSsoCode(user.getUsername(), ssoProperties.getCodeTtlSeconds());
        response.sendRedirect(request.getContextPath() + "/login/sso?code=" + code);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        invalidateSession(request);
        log.warn("SSO login failed: {}", exception.getMessage());
        refuse(request, response, null, IDP_ERROR);
    }

    private void refuse(HttpServletRequest request, HttpServletResponse response, String identity, String reason) throws IOException {
        AuditLog.log("auth.sso-login-refused", "identity={} reason={}", identity, reason);
        response.sendRedirect(request.getContextPath() + "/login?sso_error=" + URLEncoder.encode(reason, StandardCharsets.UTF_8));
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null)
            session.invalidate();
    }
}
