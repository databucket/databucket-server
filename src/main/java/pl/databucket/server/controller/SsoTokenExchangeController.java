package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.configuration.SsoProperties;
import pl.databucket.server.dto.AuthRespDTO;
import pl.databucket.server.dto.SsoTokenExchangeReqDTO;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.repository.UserRepository;
import pl.databucket.server.security.AuthResponseBuilder;
import pl.databucket.server.security.SsoRobotTokenVerifier;
import pl.databucket.server.service.UserService;

/**
 * Sign-in of robots (test automation) without a password: the robot gets an access token from the SSO provider
 * with the client credentials grant and exchanges it here for a Databucket token.
 */
@Tag(name = "PUBLIC", description = "Public endpoints for authentication and user registration")
@RestController
@RequestMapping("/api/public/sso")
public class SsoTokenExchangeController {

    @Autowired
    private SsoProperties ssoProperties;

    @Autowired
    private SsoRobotTokenVerifier robotTokenVerifier;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthResponseBuilder authResponseBuilder;

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(SsoTokenExchangeController.class);

    @Operation(
            summary = "Sign in a robot with a client token",
            description = """
                    For robots (test automation) when single sign-on is enabled: signs in with an access token the robot \
                    got from the SSO provider with the **client credentials** grant, instead of a password.

                    The token must be signed by the provider, not expired and issued for Databucket (audience: \
                    `sso.robot-audience`, by default `sso.client-id`). Its claim `sso.robot-claim` (by default `azp`, \
                    the client ID) must equal the user name of an enabled Databucket user with the **ROBOT** role.

                    The response is the same as for /api/public/signin: with **projectId** the token for that project; \
                    without it the token for the only project, or the list of `projects` to choose from (then send the \
                    provider's token again with the projectId)."""
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - the token, or the list of projects to choose from",
                    content = @Content(schema = @Schema(implementation = AuthRespDTO.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - the token is missing"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - the token is invalid, expired or not for Databucket, "
                    + "or there is no enabled user with that name"),
            @ApiResponse(responseCode = "403", description = "Forbidden - the user is not a ROBOT, has no access to the project, "
                    + "or the user or the project has expired"),
            @ApiResponse(responseCode = "404", description = "Not Found - single sign-on is not enabled"),
            @ApiResponse(responseCode = "503", description = "Service Unavailable - the SSO provider can't be reached")
    })
    @PostMapping(value = "/token-exchange", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> exchangeToken(@RequestBody SsoTokenExchangeReqDTO request) {
        if (!ssoProperties.isEnabled())
            return exceptionFormatter.customPublicException("Single sign-on is not enabled", HttpStatus.NOT_FOUND);
        if (request.getToken() == null || request.getToken().isBlank())
            return exceptionFormatter.customPublicException("The token is missing", HttpStatus.BAD_REQUEST);

        String robot;
        try {
            robot = robotTokenVerifier.verify(request.getToken());
        } catch (SsoRobotTokenVerifier.ProviderUnavailableException e) {
            AuditLog.log("auth.robot-signin-failed", "reason=provider unavailable");
            return exceptionFormatter.customPublicException(e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (JwtException e) {
            AuditLog.log("auth.robot-signin-failed", "reason={}", e.getMessage());
            return exceptionFormatter.customPublicException("The token is not valid for Databucket", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findWithProjectsAndRolesByUsername(robot);
        if (user == null || !user.getEnabled()) {
            AuditLog.log("auth.robot-signin-failed", "user={} reason={}", robot, user == null ? "unknown user" : "disabled");
            return exceptionFormatter.customPublicException("There is no enabled Databucket user " + robot, HttpStatus.UNAUTHORIZED);
        }
        if (!user.isRobot()) {
            AuditLog.log("auth.robot-signin-failed", "user={} reason=not a robot", robot);
            return exceptionFormatter.customPublicException("Only users with the ROBOT role can sign in with a client token", HttpStatus.FORBIDDEN);
        }

        try {
            UserDetails userDetails = userService.loadUserByUsername(robot);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            return authResponseBuilder.build(user, authentication, request.getProjectId(), false, "auth.robot-signin");
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }
}
