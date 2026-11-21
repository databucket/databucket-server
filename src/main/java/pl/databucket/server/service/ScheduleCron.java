package pl.databucket.server.service;

import org.springframework.scheduling.support.CronExpression;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * The cron of a task schedule: 5 fields (minute hour day month weekday, as in Unix) or 6 (with the second first, as in
 * Spring), e.g. "0 2 * * *" = every day at 2:00. Runs are at least a minute apart: the scheduler looks for due
 * schedules once a minute.
 */
public final class ScheduleCron {

    public static final Duration MIN_INTERVAL = Duration.ofMinutes(1);

    private ScheduleCron() {
    }

    public static class InvalidCronException extends Exception {
        public InvalidCronException(String message) {
            super(message);
        }
    }

    /** The cron as stored: 6 fields. */
    public static String normalize(String cron) throws InvalidCronException {
        if (cron == null || cron.isBlank())
            throw new InvalidCronException("The cron is missing, e.g. \"0 2 * * *\" (every day at 2:00)");
        String trimmed = cron.trim().replaceAll("\s+", " ");
        int fields = trimmed.split(" ").length;
        if (fields == 5)
            trimmed = "0 " + trimmed;
        else if (fields != 6)
            throw new InvalidCronException("The cron '" + cron + "' has " + fields + " fields; give 5 (minute hour day "
                    + "month weekday), e.g. \"0 2 * * *\" (every day at 2:00)");
        CronExpression expression;
        try {
            expression = CronExpression.parse(trimmed);
        } catch (IllegalArgumentException e) {
            throw new InvalidCronException("The cron '" + cron + "' is invalid: " + e.getMessage());
        }
        // two consecutive runs from now: closer than a minute = more often than the scheduler looks
        ZonedDateTime first = expression.next(ZonedDateTime.now(ZoneId.of("UTC")));
        ZonedDateTime second = first != null ? expression.next(first) : null;
        if (first == null)
            throw new InvalidCronException("The cron '" + cron + "' never runs");
        if (second != null && Duration.between(first, second).compareTo(MIN_INTERVAL) < 0)
            throw new InvalidCronException("The cron '" + cron + "' runs more often than once a minute");
        return trimmed;
    }

    public static ZoneId zone(String timeZone) throws InvalidCronException {
        if (timeZone == null || timeZone.isBlank())
            return ZoneId.of("UTC");
        try {
            return ZoneId.of(timeZone.trim());
        } catch (DateTimeException e) {
            throw new InvalidCronException("Unknown time zone '" + timeZone + "', e.g. Europe/Warsaw or UTC");
        }
    }

    /** The next run after the given moment, in the time zone of the schedule; null if it never runs again. */
    public static Instant next(String cron, String timeZone, Instant after) {
        ZonedDateTime next = CronExpression.parse(cron).next(after.atZone(ZoneId.of(timeZone)));
        return next != null ? next.toInstant() : null;
    }
}
