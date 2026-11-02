package pl.databucket.server.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Answers exceptions that controllers do not catch themselves. Without it they are forwarded to /error,
 * which runs without the JWT authentication, so the client got 401 instead of the real error.
 * Standard Spring MVC exceptions keep their status (400, 404, 405...) from the base class.
 */
@RestControllerAdvice
public class UnhandledExceptionHandler extends ResponseEntityExceptionHandler {

    private final ExceptionFormatter exceptionFormatter = new ExceptionFormatter(UnhandledExceptionHandler.class);

    // Rethrown as is, so Spring Security still answers them with 401 or 403.
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public void rethrowSecurityException(RuntimeException e) {
        throw e;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnhandledException(Exception e) {
        return exceptionFormatter.defaultException(e);
    }

    // A readable message: the fields and what is wrong with them (e.g. "icon.svg: The icon contains a forbidden element: style")
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isEmpty())
            return super.handleMethodArgumentNotValid(ex, headers, status, request);
        ResponseEntity<Map<String, Object>> response = exceptionFormatter.customException(message, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(response.getBody(), headers, HttpStatus.BAD_REQUEST);
    }

    // Same {"message": ...} body as the rest of the API instead of the base class ProblemDetail
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Map<String, Object>> response = exceptionFormatter.customException(ex, HttpStatus.valueOf(statusCode.value()));
        return new ResponseEntity<>(response.getBody(), headers, statusCode);
    }
}
