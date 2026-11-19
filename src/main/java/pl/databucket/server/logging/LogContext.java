package pl.databucket.server.logging;

/**
 * MDC keys, named after Elastic Common Schema fields so the ECS JSON log maps them onto the standard
 * fields. They also appear in the text log pattern (application.yaml, logging.pattern.console).
 */
public final class LogContext {

    public static final String REQUEST_ID = "http.request.id";
    public static final String USER = "user.name";
    // ECS has no project field; custom fields go under the application's own prefix
    public static final String PROJECT = "databucket.project.id";
    public static final String ACTION = "event.action";

    public static final String HTTP_METHOD = "http.request.method";
    public static final String HTTP_PATH = "url.path";
    public static final String HTTP_STATUS = "http.response.status_code";
    // ECS event.duration is in nanoseconds
    public static final String DURATION_NANOS = "event.duration";
    public static final String CLIENT = "client.ip";

    private LogContext() {
    }
}
