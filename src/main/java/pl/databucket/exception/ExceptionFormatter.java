package pl.databucket.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

public class ExceptionFormatter {

    private final Logger logger;
    public ExceptionFormatter(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public ResponseEntity<Map<String, Object>> defaultException(Exception e) {
        logger.error(e.getMessage(), e);
        Map<String, Object> response = new HashMap<>();
        response.put("message", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ResponseEntity<Map<String, Object>> customException(Exception e, HttpStatus status) {
        logger.warn(e.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("message", e.getMessage());
        return new ResponseEntity<>(response, status);
    }

}
