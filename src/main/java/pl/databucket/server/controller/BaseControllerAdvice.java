package pl.databucket.server.controller;

import java.util.Map;
import javax.mail.MessagingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import pl.databucket.server.dto.AuthRespDTO;
import pl.databucket.server.exception.AuthForbiddenException;
import pl.databucket.server.exception.ExceptionFormatter;
import pl.databucket.server.exception.ForbiddenRepetitionException;

@ControllerAdvice
public class BaseControllerAdvice extends ResponseEntityExceptionHandler {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(BasicAuthController.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleError(Exception ex) {
        return exceptionFormatter.defaultException(ex);
    }

    @ExceptionHandler(AuthForbiddenException.class)
    public ResponseEntity<AuthRespDTO> handleError(AuthForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getResponse());
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

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<Object> handleAccessDeniedException(
        Exception ex, WebRequest request) {
        return new ResponseEntity<Object>(
            "Access denied", new HttpHeaders(), HttpStatus.FORBIDDEN);
    }
}
