package pl.databucket.server.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.mail.MessagingException;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import pl.databucket.server.configuration.AppProperties;
import pl.databucket.server.dto.AuthRespDTO;
import pl.databucket.server.dto.ForgotPasswordReqDTO;
import pl.databucket.server.dto.ReCaptchaSiteVerifyResponseDTO;
import pl.databucket.server.dto.SignUpDtoRequest;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ForbiddenRepetitionException;
import pl.databucket.server.repository.UserRepository;
import pl.databucket.server.security.AuthResponseBuilder;
import pl.databucket.server.service.ManageUserService;
import pl.databucket.server.service.UserService;

@Tag(name = "PUBLIC")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Log4j2
public class BasicAuthController {

    UserRepository userRepository;
    UserService userService;
    ManageUserService manageUserService;
    AppProperties appProperties;
    ClientRegistrationRepository clientRegistrationRepository;
    AuthResponseBuilder authResponseBuilder;
    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(BasicAuthController.class);

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
                .collect(Collectors.toMap(
                    reg -> new StringJoiner("/").add(
                            OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI)
                        .add(reg.getRegistrationId()).toString(),
                    ClientRegistration::getRegistrationId));
        }
        return Map.of();
    }

    @GetMapping("/user-info")
    public AuthRespDTO getUserInfo(@NotNull JwtAuthenticationToken tokenAuth,
        @RequestParam(required = false) Integer projectId) {
        return Optional.ofNullable(userService.loadUserByUsername(tokenAuth.getName()))
            .map(user -> {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    user, "", user.getAuthorities());
                return authResponseBuilder.buildAuthResponse(auth, Optional.ofNullable(projectId).orElse(0).toString());
            })
            .orElseGet(() -> AuthRespDTO.builder()
                .message("User not found")
                .build());
    }


}
