package pl.databucket.server.exception;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;

import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the client is told about an exception. Database errors are translated into a short message without the SQL
 * statement (Spring puts the whole statement, with the values, into the message of a DataAccessException); the server
 * log keeps the full exception.
 */
public record ApiError(String message, HttpStatus status) {

    private static final Pattern TAG_KEY = Pattern.compile("\\(tag_id\\)=\\((\\d+)\\)");

    /**
     * @param status the status the caller chose; a database error caused by the request (wrong value, rule or
     *               reference) answers 400 or 409 instead of 500
     */
    public static ApiError of(Exception e, HttpStatus status) {
        PSQLException psql = findPsqlException(e);
        if (psql != null)
            return database(psql, status);
        if (e instanceof DataAccessException || findCause(e, SQLException.class) != null)
            return new ApiError("Database error", HttpStatus.INTERNAL_SERVER_ERROR);
        String message = e.getMessage();
        if (message == null || message.isBlank())
            message = "Unexpected error: " + e.getClass().getSimpleName();
        return new ApiError(message, status);
    }

    private static ApiError database(PSQLException e, HttpStatus status) {
        ServerErrorMessage server = e.getServerErrorMessage();
        String state = e.getSQLState() != null ? e.getSQLState() : "";
        String text = server != null && server.getMessage() != null ? server.getMessage() : "";
        String detail = server != null ? server.getDetail() : null;

        // a tag of the data that doesn't exist (foreign key of the bucket tables to tags)
        if (state.equals("23503") && detail != null && detail.contains("\"tags\"")) {
            Matcher tag = TAG_KEY.matcher(detail);
            String id = tag.find() ? " " + tag.group(1) : "";
            return new ApiError("The tag" + id + " doesn't exist", requestError(status, HttpStatus.BAD_REQUEST));
        }
        if (state.startsWith("23"))
            return new ApiError("The data conflicts with other data" + (detail != null ? ": " + detail : ""),
                    requestError(status, HttpStatus.CONFLICT));
        if (text.contains("cannot cast jsonb null"))
            return new ApiError("Failed to operate on an empty property", requestError(status, HttpStatus.BAD_REQUEST));
        // a value that doesn't fit the type used in a rule, an invalid regular expression...
        if (state.startsWith("22"))
            return new ApiError("Invalid value: " + text, requestError(status, HttpStatus.BAD_REQUEST));
        // a rule that can't be applied to the data, e.g. an operator for other types (operator does not exist: ...)
        if (state.startsWith("42"))
            return new ApiError("The rules can't be applied to the data: " + text, requestError(status, HttpStatus.BAD_REQUEST));
        if (state.equals("57014"))
            return new ApiError("The query took too long and was cancelled", HttpStatus.SERVICE_UNAVAILABLE);
        return new ApiError("Database error", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // the status the caller chose for an error of the request, or the given one instead of 500
    private static HttpStatus requestError(HttpStatus chosen, HttpStatus instead) {
        return chosen.is5xxServerError() ? instead : chosen;
    }

    // the PostgreSQL error behind the exception; a batch reports it as the next exception
    private static PSQLException findPsqlException(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof BatchUpdateException batch && batch.getNextException() instanceof PSQLException next)
                return next;
            if (t instanceof PSQLException psql)
                return psql;
        }
        return null;
    }

    private static <T extends Throwable> T findCause(Throwable e, Class<T> type) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause())
            if (type.isInstance(t))
                return type.cast(t);
        return null;
    }
}
