package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.configuration.SsoProperties;
import pl.databucket.server.dto.SsoRegistrationInfoDTO;
import pl.databucket.server.dto.SsoRegistrationReqDTO;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.security.SsoCodeService;
import pl.databucket.server.service.ManageUserService;
import pl.databucket.server.service.RegistrationService;

import java.util.Map;

/**
 * sso.self-registration: someone who signed in at the SSO provider without a Databucket account creates one
 * (MEMBER, no projects). The ticket from the /login/sso?register= redirect proves the SSO login and the email.
 */
@Hidden // used only by the web application
@RestController
@RequestMapping("/api/public/sso")
public class SsoRegistrationController {

    static final String EXPIRED = "The registration has expired. Sign in with single sign-on again.";

    @Autowired
    private SsoProperties ssoProperties;

    @Autowired
    private SsoCodeService ssoCodeService;

    @Autowired
    private ManageUserService manageUserService;

    @Autowired
    private RegistrationService registrationService;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(SsoRegistrationController.class);

    /** The registration form: the email of the ticket, a suggested user name and the public projects. */
    @PostMapping(value = "/registration", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getRegistration(@RequestBody SsoRegistrationReqDTO request) {
        String email;
        try {
            email = ssoCodeService.readRegistrationTicket(request.getTicket());
        } catch (IllegalArgumentException e) {
            return exceptionFormatter.customPublicException(EXPIRED, HttpStatus.UNAUTHORIZED);
        }
        if (!ssoProperties.isSelfRegistrationAllowed(email))
            return exceptionFormatter.customPublicException("Creating an account is not allowed.", HttpStatus.FORBIDDEN);

        return ResponseEntity.ok(new SsoRegistrationInfoDTO(email, manageUserService.suggestUsername(email),
                registrationService.getPublicProjects()));
    }

    /** Creates the account; the ticket works once. The SUPER admins get an email with the chosen projects. */
    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(@RequestBody SsoRegistrationReqDTO request) {
        String email;
        try {
            email = ssoCodeService.readRegistrationTicket(request.getTicket());
        } catch (IllegalArgumentException e) {
            AuditLog.log("auth.sso-signup-failed", "reason={}", e.getMessage());
            return exceptionFormatter.customPublicException(EXPIRED, HttpStatus.UNAUTHORIZED);
        }
        if (!ssoProperties.isSelfRegistrationAllowed(email)) {
            AuditLog.log("auth.sso-signup-failed", "email={} reason=not allowed", email);
            return exceptionFormatter.customPublicException("Creating an account is not allowed.", HttpStatus.FORBIDDEN);
        }

        try {
            // check the user name before using up the ticket, so a taken name can be corrected
            manageUserService.checkNewSsoUser(email, request.getUsername());
            ssoCodeService.redeemRegistrationTicket(request.getTicket());
            User user = manageUserService.createSsoUser(email, request.getUsername());
            registrationService.notifySuperAdmins(user, request.getProjectsIds(), request.getMessage(), "single sign-on");
            return new ResponseEntity<>(Map.of("username", user.getUsername()), HttpStatus.CREATED);
        } catch (ItemAlreadyExistsException e) {
            return exceptionFormatter.customPublicException("The user name '" + request.getUsername().trim()
                    + "' is already taken. Choose another one.", HttpStatus.CONFLICT);
        } catch (IllegalStateException e) {
            return exceptionFormatter.customPublicException(e.getMessage(), HttpStatus.CONFLICT);
        } catch (IllegalArgumentException e) {
            // an invalid user name, or the ticket was used meanwhile
            String message = e.getMessage() != null && e.getMessage().startsWith("Registration ticket") ? EXPIRED : e.getMessage();
            return exceptionFormatter.customPublicException(message, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
