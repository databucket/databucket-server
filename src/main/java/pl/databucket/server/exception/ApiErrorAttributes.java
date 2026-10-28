package pl.databucket.server.exception;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * Errors answered by Spring Boot's /error (401 and 403 from security, exceptions thrown in filters)
 * get the same "message" field as the rest of the API, which is what the frontend reads.
 */
@Component
public class ApiErrorAttributes extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(webRequest, options.including(ErrorAttributeOptions.Include.MESSAGE));
        Object message = attributes.get("message");
        if (message == null || message.toString().isBlank())
            attributes.put("message", attributes.get("error"));
        return attributes;
    }
}
