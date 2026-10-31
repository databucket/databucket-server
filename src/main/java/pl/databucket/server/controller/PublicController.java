package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import pl.databucket.server.configuration.AppProperties;
import pl.databucket.server.dto.*;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ForbiddenRepetitionException;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.repository.UserRepository;
import pl.databucket.server.security.AuthResponseBuilder;
import pl.databucket.server.security.TokenProvider;
import pl.databucket.server.service.ManageUserService;

import jakarta.mail.MessagingException;
import java.util.*;

@Tag(name = "Authentication", description = "Signing in. The token from the response authorizes the other calls (click Authorize).")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/public")
public class PublicController {

    Logger logger = LoggerFactory.getLogger(PublicController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManageUserService manageUserService;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private TokenProvider jwtTokenUtil;

    @Autowired
    private AuthResponseBuilder authResponseBuilder;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(PublicController.class);


    @Operation(
            summary = "Sign in",
            description = """
                    Signs in with a user name and password and returns the token for one project, with the user's roles.

                    * With **projectId**: signs in to that project.
                    * Without projectId, when the user has **one project**: signs in to it.
                    * Without projectId, when the user has **several projects**: returns the list of `projects` and no \
                    token; sign in again with the chosen projectId.

                    The token expires after 24 hours by default; then sign in again. Accounts that sign in through \
                    single sign-on can't use the API; use an account with a password, e.g. with the ROBOT role."""
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK - the token, or the list of projects to choose from",
                    content = @Content(schema = @Schema(implementation = AuthRespDTO.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - wrong user name or password, or the user is disabled"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - the user's access has expired, the project is disabled or expired, "
                            + "or the user is not assigned to the project (or to any project)"
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @PostMapping(value = {"/signin"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signIn(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The credentials and, optionally, the project", required = true)
            @RequestBody AuthReqDTO authReqDTO) {
        User user = userRepository.findWithProjectsAndRolesByUsername(authReqDTO.getUsername());
        if (user == null) {
            AuditLog.log("auth.signin-failed", "user={} reason=unknown user", authReqDTO.getUsername());
            return exceptionFormatter.customException(new UsernameNotFoundException("Bad credentials"), HttpStatus.UNAUTHORIZED);
        }

        try {
            final Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authReqDTO.getUsername(), authReqDTO.getPassword()));
            return authResponseBuilder.build(user, authentication, authReqDTO.getProjectId(), true, "auth.signin");
        } catch (AuthenticationException e) {
            AuditLog.log("auth.signin-failed", "user={} reason={}", authReqDTO.getUsername(), e.getMessage());
            return exceptionFormatter.customException(e, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @Operation(
            summary = "Forgot password",
            description = "Sends a password reset email to the user",
            hidden = true
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No Content - email sent successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - user not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - too many requests"),
            @ApiResponse(responseCode = "503", description = "Service Unavailable - mail service exception"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @Hidden
    @PostMapping(value = "/forgot-password", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> forgotPassword(
            @Parameter(description = "Forgot password request payload", required = true)
            @RequestBody ForgotPasswordReqDTO forgotPasswordReqDTO) {
        try {
            manageUserService.forgotPasswordMessage(forgotPasswordReqDTO);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (MessagingException | MailSendException e) {
            return exceptionFormatter.customPublicException("Mail service exception!", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (UsernameNotFoundException e) {
            return exceptionFormatter.customPublicException(e.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (ForbiddenRepetitionException e) {
            return exceptionFormatter.customPublicException(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @Operation(
            summary = "Forgot password confirmation",
            description = "Confirms password reset and sends new password to user",
            hidden = true
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - password reset successful"),
            @ApiResponse(responseCode = "403", description = "Forbidden - invalid or expired token"),
            @ApiResponse(responseCode = "503", description = "Service Unavailable - mail service exception"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @Hidden
    @GetMapping(value = {"/confirmation/forgot-password/{jwts}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> forgotPasswordConfirmation(
            @Parameter(description = "JWT token for password reset", required = true)
            @PathVariable String jwts) {
        try {
            manageUserService.resetAndSendPassword(jwts);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (MessagingException | MailSendException e) {
            return exceptionFormatter.customException(e, "Mail service exception!", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (ForbiddenRepetitionException | AccountExpiredException e) {
            return exceptionFormatter.customPublicException(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @Operation(
            summary = "Sign up",
            description = "Creates a new user account and sends confirmation email",
            hidden = true
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created - account created successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - reCAPTCHA verification failed"),
            @ApiResponse(responseCode = "409", description = "Conflict - username or email already exists"),
            @ApiResponse(responseCode = "503", description = "Service Unavailable - mail service exception"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @Hidden
    @PostMapping(value = {"/sign-up"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signUp(
            @Parameter(description = "Sign up request payload", required = true)
            @RequestBody SignUpDtoRequest signUpDtoRequest) {
        try {
            if (appProperties.getRecaptchaEnabled() && !checkReCaptcha(signUpDtoRequest.getRecaptchaToken()))
                return exceptionFormatter.customPublicException("Anti-bot verification blocked your request!", HttpStatus.FORBIDDEN);

            if (userRepository.existsByUsername(signUpDtoRequest.getUsername()))
                return exceptionFormatter.customPublicException("Given username already exists", HttpStatus.CONFLICT);

            if (userRepository.existsByEmailIgnoreCase(User.normalizeEmail(signUpDtoRequest.getEmail())))
                return exceptionFormatter.customPublicException("Given email already exists", HttpStatus.CONFLICT);

            manageUserService.signUpUser(signUpDtoRequest);
            return exceptionFormatter.responseMessage("An account has been created for a new user", HttpStatus.CREATED);
        } catch (MessagingException | MailSendException e) {
            return exceptionFormatter.customException(e, "Mail service exception!", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    private boolean checkReCaptcha(String token) {
        RestTemplate restTemplate = new RestTemplate();

        String uri = "https://www.google.com/recaptcha/api/siteverify" +
                "?secret=" + appProperties.getRecaptchaSecretKey() +
                "&response=" + token;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<?> result = restTemplate.exchange(uri, HttpMethod.GET, entity, ReCaptchaSiteVerifyResponseDTO.class);
        ReCaptchaSiteVerifyResponseDTO reCaptchaSiteVerifyResponse = (ReCaptchaSiteVerifyResponseDTO) result.getBody();
        assert reCaptchaSiteVerifyResponse != null;

        if (!reCaptchaSiteVerifyResponse.isSuccess())
            logger.error("ReCaptcha failed: " + Arrays.toString(reCaptchaSiteVerifyResponse.getErrorCodes()));

        return reCaptchaSiteVerifyResponse.isSuccess() && reCaptchaSiteVerifyResponse.getScore() > 0.5;
    }

    @Operation(
            summary = "Sign up confirmation",
            description = "Confirms user registration via email token",
            hidden = true
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - account confirmed successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - invalid or expired token"),
            @ApiResponse(responseCode = "503", description = "Service Unavailable - mail service exception"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @Hidden
    @GetMapping(value = {"/confirmation/sign-up/{jwts}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signUpConfirmation(
            @Parameter(description = "JWT token for sign up confirmation", required = true)
            @PathVariable String jwts) {
        try {
            manageUserService.signUpUserConfirmation(jwts);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (MessagingException | MailSendException e) {
            return exceptionFormatter.customException(e, "Mail service exception!", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (ForbiddenRepetitionException | AccountExpiredException e) {
            return exceptionFormatter.customPublicException(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @Operation(
            summary = "Get reCAPTCHA site key",
            description = "Returns the reCAPTCHA site key and enabled status",
            hidden = true
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @Hidden
    @GetMapping(value = {"/recaptcha-site-key"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getReCaptchaSiteKey() {
        try {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("enabled", appProperties.getRecaptchaEnabled());
            if (appProperties.getRecaptchaEnabled())
                responseBody.put("siteKey", appProperties.getRecaptchaSiteKey());
            else
                responseBody.put("siteKey", null);

            return new ResponseEntity<>(responseBody, HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

}
