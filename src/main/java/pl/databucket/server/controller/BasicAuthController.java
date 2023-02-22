package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.mail.MessagingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import pl.databucket.server.configuration.AppProperties;
import pl.databucket.server.dto.AuthProjectDTO;
import pl.databucket.server.dto.AuthReqDTO;
import pl.databucket.server.dto.AuthRespDTO;
import pl.databucket.server.dto.ForgotPasswordReqDTO;
import pl.databucket.server.dto.ReCaptchaSiteVerifyResponseDTO;
import pl.databucket.server.dto.SignUpDtoRequest;
import pl.databucket.server.entity.Project;
import pl.databucket.server.entity.User;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ForbiddenRepetitionException;
import pl.databucket.server.repository.UserRepository;
import pl.databucket.server.security.TokenProvider;
import pl.databucket.server.service.ManageUserService;

@Tag(name = "PUBLIC")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Log4j2
public class BasicAuthController {

    AuthenticationManager authenticationManager;
    UserRepository userRepository;
    ManageUserService manageUserService;
    ModelMapper modelMapper;
    AppProperties appProperties;
    TokenProvider jwtTokenUtil;
    ClientRegistrationRepository clientRegistrationRepository;
    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(BasicAuthController.class);


    @Operation(
        summary = "Authenticate",
        description = "Returns the token required to authorize the user, user's roles, project details if given projectId, or list of user's projects if the projectId is not given.",
        responses = {
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - the given credentials are not correct"),
            @ApiResponse(responseCode = "403", description = "Forbidden - user access has expired | the project is disabled | the project is expired | the user is not assign to given project | the user is not assign to any project"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),
        })
    @PostMapping(value = {"/sign-in", "/signin"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthRespDTO> signIn(
        @Parameter(name = "payload - username (required), password (required), projectId (optional)", required = true) @RequestBody AuthReqDTO authReqDTO) {
        User user = userRepository.findByUsername(authReqDTO.getUsername());
        if (user == null) {
            throw new UsernameNotFoundException("Bad credentials");
        }
        AuthRespDTO authDtoResponse = new AuthRespDTO();
        HttpStatus status = HttpStatus.OK;

        final Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(authReqDTO.getUsername(), authReqDTO.getPassword()));

        if (user.isExpired()) {
            authDtoResponse.setMessage("User access has expired!");
            return new ResponseEntity<>(authDtoResponse, HttpStatus.FORBIDDEN);
        }

        // The user has to change password. We force this action.
        if (user.isChangePassword()) {
            authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication,
                authReqDTO.getProjectId() != null ? authReqDTO.getProjectId() : 0));
            authDtoResponse.setChangePassword(true);

            // We've got the projectId that user want to sign into
        } else if (authReqDTO.getProjectId() != null) {

            // User is assigned to given project
            if (user.getProjects().stream().anyMatch(o -> o.getId() == authReqDTO.getProjectId())) {
                Project project = user.getProjects().stream().filter(o -> o.getId() == authReqDTO.getProjectId())
                    .findFirst().get();

                if (!project.getEnabled()) {
                    authDtoResponse.setMessage("The project is disabled!");
                    status = HttpStatus.FORBIDDEN;

                    // The project is expired
                } else if (project.isExpired()) {
                    authDtoResponse.setMessage("The project expired!");
                    status = HttpStatus.FORBIDDEN;

                    // The project is enabled and not expired
                } else {
                    AuthProjectDTO authProjectDto = new AuthProjectDTO();
                    modelMapper.map(project, authProjectDto);
                    List<String> roles = user.getRoles().stream()
                        .map(item -> modelMapper.map(item.getName(), String.class))
                        .toList();

                    authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, authReqDTO.getProjectId()));
                    authDtoResponse.setProject(authProjectDto);
                    authDtoResponse.setRoles(roles);
                }

                // The user is not assigned to requested project
            } else {
                authDtoResponse.setMessage("The user is not assigned to given project!");
                status = HttpStatus.FORBIDDEN;
            }

            // We haven't got the projectId but the user is assigned to one project.
        } else if (user.getProjects().size() == 1) {
            Project project = user.getProjects().iterator().next();

            // The project is disabled
            if (!project.getEnabled()) {
                authDtoResponse.setMessage("The project is disabled!");
                status = HttpStatus.FORBIDDEN;

                // The project is expired
            } else if (project.isExpired()) {
                authDtoResponse.setMessage("The project expired!");
                status = HttpStatus.FORBIDDEN;

                // The project is enabled and not expired
            } else {
                List<AuthProjectDTO> projects = user.getProjects().stream()
                    .map(item -> modelMapper.map(item, AuthProjectDTO.class)).toList();
                List<String> roles = user.getRoles().stream()
                    .map(item -> modelMapper.map(item.getName(), String.class)).toList();
                AuthProjectDTO authProjectDto = new AuthProjectDTO();
                modelMapper.map(project, authProjectDto);

                authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, projects.get(0).getId()));
                authDtoResponse.setProject(authProjectDto);
                authDtoResponse.setRoles(roles);
            }

            // We haven't got the projectId, and the user is assigned to more then one project. We return all projects to which the user is assigned.
        } else if (user.getProjects().size() > 1) {
            List<AuthProjectDTO> projects = user.getProjects().stream()
                .map(item -> modelMapper.map(item, AuthProjectDTO.class))
                .toList();
            List<String> roles = user.getRoles().stream()
                .map(item -> modelMapper.map(item.getName(), String.class))
                .toList();

            authDtoResponse.setProjects(projects);
            authDtoResponse.setRoles(roles);

            if (user.isSuperUser()) {
                authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, 0));
            }

            // We haven't got he projectId, and the user is not assigned to any project, but the user is the SUPER user.
        } else if (user.isSuperUser()) {
            List<String> roles = user.getRoles().stream().map(item -> modelMapper.map(item.getName(), String.class))
                .toList();

            authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, 0));
            authDtoResponse.setProjects(new ArrayList<>());
            authDtoResponse.setRoles(roles);

            // We haven't got the projectId, and the user is not assigned to any project and this user is not SUPER user.
        } else {
            authDtoResponse.setMessage("The user is not assigned to any project!");
            status = HttpStatus.FORBIDDEN;
        }

        return new ResponseEntity<>(authDtoResponse, status);
    }

    @PostMapping(value = "/forgot-password", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordReqDTO forgotPasswordReqDTO)
        throws MessagingException, ForbiddenRepetitionException {
        manageUserService.forgotPasswordMessage(forgotPasswordReqDTO);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping(value = {"/confirmation/forgot-password/{jwts}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> forgotPasswordConfirmation(@PathVariable String jwts)
        throws MessagingException, ForbiddenRepetitionException {
        manageUserService.resetAndSendPassword(jwts);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping(value = {"/sign-up"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> signUp(@RequestBody SignUpDtoRequest signUpDtoRequest)
        throws ForbiddenRepetitionException, MessagingException {
        if (appProperties.getRecaptchaEnabled() && !checkReCaptcha(signUpDtoRequest.getRecaptchaToken())) {
            throw new ForbiddenRepetitionException("Anti-bot verification blocked your request!");
        }

        if (userRepository.existsByUsername(signUpDtoRequest.getUsername())) {
            throw new ForbiddenRepetitionException("Cannot create user");
        }

        if (userRepository.existsByEmail(signUpDtoRequest.getEmail())) {
            throw new ForbiddenRepetitionException("Cannot create user");
        }

        manageUserService.signUpUser(signUpDtoRequest);
        return exceptionFormatter.responseMessage("An account has been created for a new user", HttpStatus.CREATED);
    }

    private boolean checkReCaptcha(String token) {
        RestTemplate restTemplate = new RestTemplate();

        String uri = "https://www.google.com/recaptcha/api/siteverify" +
            "?secret=" + appProperties.getRecaptchaSecretKey() +
            "&response=" + token;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        ResponseEntity<?> result = restTemplate.exchange(uri, HttpMethod.GET, entity,
            ReCaptchaSiteVerifyResponseDTO.class);
        ReCaptchaSiteVerifyResponseDTO reCaptchaSiteVerifyResponse = (ReCaptchaSiteVerifyResponseDTO) result.getBody();
        assert reCaptchaSiteVerifyResponse != null;

        if (!reCaptchaSiteVerifyResponse.isSuccess()) {
            log.error("ReCaptcha failed: " + Arrays.toString(reCaptchaSiteVerifyResponse.getErrorCodes()));
        }

        return reCaptchaSiteVerifyResponse.isSuccess() && reCaptchaSiteVerifyResponse.getScore() > 0.5;
    }

    @GetMapping(value = {"/confirmation/sign-up/{jwts}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> signUpConfirmation(@PathVariable String jwts)
        throws MessagingException, ForbiddenRepetitionException {
        manageUserService.signUpUserConfirmation(jwts);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping(value = {"/recaptcha-site-key"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getReCaptchaSiteKey() {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("enabled", appProperties.getRecaptchaEnabled());
        if (appProperties.getRecaptchaEnabled()) {
            responseBody.put("siteKey", appProperties.getRecaptchaSiteKey());
        } else {
            responseBody.put("siteKey", null);
        }
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
    }

    @GetMapping("/auth-options")
    public Map<String, String> getLoginPage() {
        Iterable<ClientRegistration> clientRegistrations = null;
        ResolvableType type = ResolvableType.forInstance(clientRegistrationRepository)
            .as(Iterable.class);
        if (type != ResolvableType.NONE &&
            ClientRegistration.class.isAssignableFrom(type.resolveGenerics()[0])) {
            clientRegistrations = (Iterable<ClientRegistration>) clientRegistrationRepository;
            return StreamSupport.stream(clientRegistrations.spliterator(), false)
                .collect(Collectors.toMap(ClientRegistration::getClientName, ClientRegistration::getRegistrationId));
        }
        return Map.of();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleError(Exception ex) {
        return exceptionFormatter.defaultException(ex);
    }

    @ExceptionHandler({ForbiddenRepetitionException.class})
    public ResponseEntity<Map<String, Object>> handleError(ForbiddenRepetitionException ex) {
        return exceptionFormatter.customPublicException(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({MessagingException.class, MailSendException.class})
    public ResponseEntity<Map<String, Object>> handleMailError() {
        return exceptionFormatter.customPublicException("Mail service exception!", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleError(AuthenticationException e) {
        return exceptionFormatter.customException(e, HttpStatus.UNAUTHORIZED);
    }
}
