package pl.databucket.server.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Runs first for every request. Gives it a requestId (taken from X-Request-Id or generated, and sent back
 * in the response), so all log lines of one request can be found together, and writes one access log
 * line per /api request. The user and project are added to the MDC by JwtAuthenticationFilter.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private static final Logger accessLogger = LoggerFactory.getLogger("pl.databucket.server.access");
    // Bucket data (rows, reservations, history) is most of the traffic, so it has its own logger
    private static final Logger dataAccessLogger = LoggerFactory.getLogger("pl.databucket.server.access.data");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank() || requestId.length() > 64)
            requestId = UUID.randomUUID().toString().substring(0, 8);

        MDC.put(LogContext.REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            if (request.getRequestURI().startsWith("/api/"))
                logAccess(request, response, System.currentTimeMillis() - start);
            MDC.clear();
        }
    }

    private void logAccess(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        // Also put into the MDC, so they are separate fields in the JSON log
        MDC.put(LogContext.HTTP_METHOD, request.getMethod());
        MDC.put(LogContext.HTTP_PATH, request.getRequestURI());
        MDC.put(LogContext.HTTP_STATUS, String.valueOf(response.getStatus()));
        MDC.put(LogContext.DURATION_NANOS, String.valueOf(durationMs * 1_000_000));
        MDC.put(LogContext.CLIENT, request.getRemoteAddr());
        Logger logger = request.getRequestURI().startsWith("/api/bucket/") ? dataAccessLogger : accessLogger;
        logger.info("{} {} {} {}ms from {}", request.getMethod(), request.getRequestURI(),
                response.getStatus(), durationMs, request.getRemoteAddr());
    }
}
