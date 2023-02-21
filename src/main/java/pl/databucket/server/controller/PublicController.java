package pl.databucket.server.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Api(tags = "PUBLIC")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class PublicController {

    Logger logger = LoggerFactory.getLogger(PublicController.class);

    AuthenticationManager authenticationManager;
    UserRepository userRepository;
    ManageUserService manageUserService;
    ModelMapper modelMapper;
    AppProperties appProperties;
    TokenProvider jwtTokenUtil;
    ClientRegistrationRepository clientRegistrationRepository;
    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(PublicController.class);


    @ApiOperation(value = "Authenticate", notes = "Returns the token required to authorize the user, user's roles, project details if given projectId, or list of user's projects if the projectId is not given.", response = AuthRespDTO.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = AuthRespDTO.class),
        @ApiResponse(code = 401, message = "Unauthorized - the given credentials are not correct"),
        @ApiResponse(code = 403, message = "Forbidden - user access has expired | the project is disabled | the project is expired | the user is not assign to given project | the user is not assign to any project"),
        @ApiResponse(code = 500, message = "Internal server error"),
    })
    @PostMapping(value = {"/sign-in", "/signin"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signIn(
        @ApiParam(value = "payload - username (required), password (required), projectId (optional)", required = true) @RequestBody AuthReqDTO authReqDTO) {
        User user = userRepository.findByUsername(authReqDTO.getUsername());
        if (user == null) {
            return exceptionFormatter.customException(new UsernameNotFoundException("Bad credentials"),
                HttpStatus.UNAUTHORIZED);
        }

        try {
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
                            .map(item -> modelMapper.map(item.getName(), String.class)).collect(Collectors.toList());

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
                        .map(item -> modelMapper.map(item, AuthProjectDTO.class)).collect(Collectors.toList());
                    List<String> roles = user.getRoles().stream()
                        .map(item -> modelMapper.map(item.getName(), String.class)).collect(Collectors.toList());
                    AuthProjectDTO authProjectDto = new AuthProjectDTO();
                    modelMapper.map(project, authProjectDto);

                    authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, projects.get(0).getId()));
                    authDtoResponse.setProject(authProjectDto);
                    authDtoResponse.setRoles(roles);
                }

                // We haven't got the projectId, and the user is assigned to more then one project. We return all projects to which the user is assigned.
            } else if (authReqDTO.getProjectId() == null && user.getProjects().size() > 1) {
                List<AuthProjectDTO> projects = user.getProjects().stream()
                    .map(item -> modelMapper.map(item, AuthProjectDTO.class)).collect(Collectors.toList());
                List<String> roles = user.getRoles().stream().map(item -> modelMapper.map(item.getName(), String.class))
                    .collect(Collectors.toList());

                authDtoResponse.setProjects(projects);
                authDtoResponse.setRoles(roles);

                if (user.isSuperUser()) {
                    authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, 0));
                }

                // We haven't got he projectId, and the user is not assigned to any project, but the user is the SUPER user.
            } else if (authReqDTO.getProjectId() == null && user.getProjects().size() == 0 && user.isSuperUser()) {
                List<String> roles = user.getRoles().stream().map(item -> modelMapper.map(item.getName(), String.class))
                    .collect(Collectors.toList());

                authDtoResponse.setToken(jwtTokenUtil.generateToken(authentication, 0));
                authDtoResponse.setProjects(new ArrayList<>());
                authDtoResponse.setRoles(roles);

                // We haven't got the projectId, and the user is not assigned to any project and this user is not SUPER user.
            } else {
                authDtoResponse.setMessage("The user is not assigned to any project!");
                status = HttpStatus.FORBIDDEN;
            }

            return new ResponseEntity<>(authDtoResponse, status);

        } catch (AuthenticationException e) {
            return exceptionFormatter.customException(authReqDTO.getUsername(), e, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PostMapping(value = "/forgot-password", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordReqDTO forgotPasswordReqDTO) {
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

    @GetMapping(value = {"/confirmation/forgot-password/{jwts}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> forgotPasswordConfirmation(@PathVariable String jwts) {
        try {
            manageUserService.resetAndSendPassword(jwts);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (MessagingException | MailSendException e) {
            return exceptionFormatter.customException("Mail service exception!", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (ForbiddenRepetitionException e) {
            return exceptionFormatter.customPublicException(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @PostMapping(value = {"/sign-up"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signUp(@RequestBody SignUpDtoRequest signUpDtoRequest) {
        try {
            if (appProperties.getRecaptchaEnabled() && !checkReCaptcha(signUpDtoRequest.getRecaptchaToken())) {
                return exceptionFormatter.customPublicException("Anti-bot verification blocked your request!",
                    HttpStatus.FORBIDDEN);
            }

            if (userRepository.existsByUsername(signUpDtoRequest.getUsername())) {
                return exceptionFormatter.customPublicException("Given username already exists", HttpStatus.CONFLICT);
            }

            if (userRepository.existsByEmail(signUpDtoRequest.getEmail())) {
                return exceptionFormatter.customPublicException("Given email already exists", HttpStatus.CONFLICT);
            }

            manageUserService.signUpUser(signUpDtoRequest);
            return exceptionFormatter.responseMessage("An account has been created for a new user", HttpStatus.CREATED);
        } catch (MessagingException | MailSendException e) {
            return exceptionFormatter.customException("Mail service exception!", HttpStatus.SERVICE_UNAVAILABLE);
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
        ResponseEntity<?> result = restTemplate.exchange(uri, HttpMethod.GET, entity,
            ReCaptchaSiteVerifyResponseDTO.class);
        ReCaptchaSiteVerifyResponseDTO reCaptchaSiteVerifyResponse = (ReCaptchaSiteVerifyResponseDTO) result.getBody();
        assert reCaptchaSiteVerifyResponse != null;

        if (!reCaptchaSiteVerifyResponse.isSuccess()) {
            logger.error("ReCaptcha failed: " + Arrays.toString(reCaptchaSiteVerifyResponse.getErrorCodes()));
        }

        return reCaptchaSiteVerifyResponse.isSuccess() && reCaptchaSiteVerifyResponse.getScore() > 0.5;
    }

    @GetMapping(value = {"/confirmation/sign-up/{jwts}"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signUpConfirmation(@PathVariable String jwts) {
        try {
            manageUserService.signUpUserConfirmation(jwts);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (MessagingException | MailSendException e) {
            return exceptionFormatter.customException("Mail service exception!", HttpStatus.SERVICE_UNAVAILABLE);
        } catch (ForbiddenRepetitionException e) {
            return exceptionFormatter.customPublicException(e.getMessage(), HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
    }

    @GetMapping(value = {"/recaptcha-site-key"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getReCaptchaSiteKey() {
        try {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("enabled", appProperties.getRecaptchaEnabled());
            if (appProperties.getRecaptchaEnabled()) {
                responseBody.put("siteKey", appProperties.getRecaptchaSiteKey());
            } else {
                responseBody.put("siteKey", null);
            }

            return new ResponseEntity<>(responseBody, HttpStatus.OK);
        } catch (Exception e) {
            return exceptionFormatter.defaultException(e);
        }
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
                .collect(Collectors.toMap(reg -> reg.getClientName(), reg -> reg.getRegistrationId()));
        }
        return Map.of();
//        clientRegistrations.forEach(registration ->
//            oauth2AuthenticationUrls.put(registration.getClientName(),
//                authorizationRequestBaseUri + "/" + registration.getRegistrationId()));
//        model.addAttribute("urls", oauth2AuthenticationUrls);
//        return "oauth_login";
    }
}
