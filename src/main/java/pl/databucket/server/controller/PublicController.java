package pl.databucket.server.controller;

import io.swagger.annotations.Api;
import javax.mail.MessagingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ForbiddenRepetitionException;
import pl.databucket.server.service.ManageUserService;

@Api(tags = "PUBLIC")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class PublicController {

    ManageUserService manageUserService;
    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(PublicController.class);


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


}
