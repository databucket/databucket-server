package pl.databucket.server.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the {"message": ...} error responses and logs them. The user, project and request id are in the
 * log context of every line, so they are not repeated in the message. Client errors (4xx) are logged as INFO
 * without a stack trace, as the access log already has their status; server errors (5xx) as ERROR.
 */
public class ExceptionFormatter {

    private final Logger logger;

    public ExceptionFormatter(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    // An unexpected exception: 500, logged with its stack trace
    public ResponseEntity<Map<String, Object>> defaultException(Exception e) {
        logger.error(e.getMessage(), e);
        return response(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ResponseEntity<Map<String, Object>> customException(Exception e, HttpStatus status) {
        log(status, e.getMessage(), null);
        return response(e.getMessage(), status);
    }

    public ResponseEntity<Map<String, Object>> customException(String message, HttpStatus status) {
        log(status, message, null);
        return response(message, status);
    }

    // The client gets the message, the log also gets the cause (e.g. why the mail could not be sent)
    public ResponseEntity<Map<String, Object>> customException(Exception cause, String message, HttpStatus status) {
        log(status, message + ": " + cause.getMessage(), cause);
        return response(message, status);
    }

    public ResponseEntity<Map<String, Object>> customPublicException(String message, HttpStatus status) {
        return response(message, status);
    }

    public ResponseEntity<Map<String, Object>> responseMessage(String message, HttpStatus status) {
        return response(message, status);
    }

    private void log(HttpStatus status, String message, Exception cause) {
        if (status.is5xxServerError())
            logger.error(message, cause);
        else
            logger.info(message);
    }

    private ResponseEntity<Map<String, Object>> response(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return new ResponseEntity<>(response, status);
    }
}
