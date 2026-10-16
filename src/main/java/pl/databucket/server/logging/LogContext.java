package pl.databucket.server.logging;

/**
 * MDC keys. They appear in the text log pattern and as fields in the JSON log.
 */
public final class LogContext {

    public static final String REQUEST_ID = "requestId";
    public static final String USER = "user";
    public static final String PROJECT = "project";

    public static final String HTTP_METHOD = "httpMethod";
    public static final String HTTP_PATH = "httpPath";
    public static final String HTTP_STATUS = "httpStatus";
    public static final String DURATION_MS = "durationMs";
    public static final String CLIENT = "client";

    private LogContext() {
    }
}
