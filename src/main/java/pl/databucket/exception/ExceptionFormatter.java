package pl.databucket.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;

public class ExceptionFormatter {

    private final Logger logger;
    public ExceptionFormatter(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public ResponseEntity<Map<String, Object>> defaultException(Exception e) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.error("[" + userName + "] " + e.getMessage(), e);
        Map<String, Object> response = new HashMap<>();
        response.put("message", e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ResponseEntity<Map<String, Object>> customException(String userName, Exception e, HttpStatus status) {
        logger.warn("[" + userName + "] " + e.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("message", e.getMessage());
        return new ResponseEntity<>(response, status);
    }

    public ResponseEntity<Map<String, Object>> customException(Exception e, HttpStatus status) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.warn("[" + userName + "] " + e.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("message", e.getMessage());
        return new ResponseEntity<>(response, status);
    }

    public ResponseEntity<Map<String, Object>> customException(String message, HttpStatus status) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.warn("[" + userName + "] " + message);
        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        return new ResponseEntity<>(response, status);
    }

}
