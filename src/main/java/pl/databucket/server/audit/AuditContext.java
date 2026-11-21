package pl.databucket.server.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.logging.LogContext;
import pl.databucket.server.security.CustomUserDetails;

/** Who, from where and in which project: what every audit event records of the current request or thread. */
final class AuditContext {

    static final String CLIENT_HEADER = "Databucket-Client";

    private AuditContext() {
    }

    /** The signed-in user, or "system" (startup, imports, nightly jobs). */
    static String actor() {
        Authentication authentication = signedIn();
        return authentication != null && authentication.getName() != null ? authentication.getName() : "system";
    }

    // the authentication of a signed-in user; not the anonymous one of the public endpoints (sign-in)
    private static Authentication signedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof AnonymousAuthenticationToken ? null : authentication;
    }

    /** The highest role of the signed-in user (the token carries the implied roles too). */
    static String role() {
        Authentication authentication = signedIn();
        if (authentication == null)
            return null;
        String top = null;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String name = authority.getAuthority() != null && authority.getAuthority().startsWith("ROLE_")
                    ? authority.getAuthority().substring(5) : null;
            if (name != null && Constants.roleLevel(name) > (top == null ? -1 : Constants.roleLevel(top)))
                top = name;
        }
        return top;
    }

    /** The project of the signed-in user, null outside a project. */
    static Integer project() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details)
            return details.getProjectId();
        return null;
    }

    /**
     * Where the change came from: the web application (its header), the MCP server, the API; without a request the
     * scheduler (its threads) or the server itself (startup, imports).
     */
    static String source() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String path = request.getRequestURI();
            if (path != null && path.contains("/mcp"))
                return "MCP";
            return "web".equalsIgnoreCase(request.getHeader(CLIENT_HEADER)) ? "WEB" : "API";
        }
        return Thread.currentThread().getName().startsWith("scheduling") ? "SCHEDULER" : "SYSTEM";
    }

    static String requestId() {
        return MDC.get(LogContext.REQUEST_ID);
    }

    static String truncate(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }
}
