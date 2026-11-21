package pl.databucket.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import pl.databucket.server.dto.TaskRunDTO;
import pl.databucket.server.dto.TaskRunResponse;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.security.CustomUserDetails;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs task schedules: picks the due ones of all projects (plain SQL, outside the project filter), moves each one's
 * next run forward in the same transaction (SELECT ... FOR UPDATE SKIP LOCKED: with several instances each schedule is
 * taken by one of them) and then runs its task as the user who saved it, in its project - the same security context a
 * request with that user's token gets, so the access checks, the roles and the project filter apply as usual. Every
 * run is recorded (task_schedule_runs, the last* fields of the schedule); after max-failures failed runs in a row the
 * schedule is disabled.
 */
@Service
public class TaskScheduleRunner {

    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
    public static final String SCHEDULE = "SCHEDULE";
    public static final String MANUAL = "MANUAL";

    private static final Logger logger = LoggerFactory.getLogger(TaskScheduleRunner.class);
    private static final int MESSAGE_MAX = 1000;
    private static final int CLAIM_BATCH = 20;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final TaskRunService taskRunService;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final long maxRows;
    private final int maxFailures;
    private final int historySize;

    public TaskScheduleRunner(NamedParameterJdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager,
                              TaskRunService taskRunService, UserService userService, ObjectMapper objectMapper,
                              @Value("${databucket.task-schedules.max-rows:10000}") long maxRows,
                              @Value("${databucket.task-schedules.max-failures:5}") int maxFailures,
                              @Value("${databucket.task-schedules.history-size:100}") int historySize) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // its own transaction also when called from one ("Run now" records the run even if the caller rolls back)
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.taskRunService = taskRunService;
        this.userService = userService;
        this.objectMapper = objectMapper;
        this.maxRows = maxRows;
        this.maxFailures = maxFailures;
        this.historySize = historySize;
    }

    /** A schedule as it runs: read with plain SQL, as the scheduler has no project. */
    record Schedule(long id, int projectId, String name, long taskId, String bucketName, List<Object> rules,
                    String cron, String timeZone, String runAs) {
    }

    /** The result of one run, as recorded. */
    public record Outcome(String status, Long count, String message) {
    }

    /** Runs the due schedules of all projects; returns how many ran. */
    public int runDue() {
        int ran = 0;
        List<Long> claimed;
        do {
            claimed = claimDue(Instant.now());
            for (Long id : claimed) {
                Schedule schedule = load(id);
                if (schedule != null) {
                    run(schedule, SCHEDULE);
                    ran++;
                }
            }
        } while (claimed.size() == CLAIM_BATCH);
        return ran;
    }

    /** "Run now": runs the schedule at once, as its run-as user, whether enabled or not. */
    public Outcome runNow(long scheduleId) {
        Schedule schedule = load(scheduleId);
        if (schedule == null)
            return new Outcome(ERROR, null, "The schedule doesn't exist");
        return run(schedule, MANUAL);
    }

    // the due schedules, with their next run moved forward (from now: runs missed while the server was down are made
    // up for by one run, not one per missed time)
    List<Long> claimDue(Instant now) {
        List<Long> claimed = transactionTemplate.execute(status -> {
            List<Map<String, Object>> due = jdbcTemplate.queryForList(
                    "SELECT schedule_id, cron, time_zone FROM task_schedules"
                            + " WHERE enabled = true AND deleted = false AND next_run_at <= :now"
                            + " ORDER BY next_run_at LIMIT " + CLAIM_BATCH + " FOR UPDATE SKIP LOCKED",
                    new MapSqlParameterSource("now", Timestamp.from(now)));
            List<Long> ids = new ArrayList<>();
            for (Map<String, Object> row : due) {
                long id = ((Number) row.get("schedule_id")).longValue();
                Instant next;
                try {
                    next = ScheduleCron.next((String) row.get("cron"), (String) row.get("time_zone"), now);
                } catch (RuntimeException e) {
                    next = null; // a cron or zone that no longer parses (saved by hand): run once, then stop
                }
                jdbcTemplate.update("UPDATE task_schedules SET next_run_at = :next WHERE schedule_id = :id",
                        new MapSqlParameterSource("next", next != null ? Timestamp.from(next) : null).addValue("id", id));
                ids.add(id);
            }
            return ids;
        });
        return claimed != null ? claimed : List.of();
    }

    Schedule load(long id) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT s.schedule_id, s.project_id, s.schedule_name, s.task_id, b.bucket_name, s.rules::text AS rules,"
                        + " s.cron, s.time_zone, s.run_as"
                        + " FROM task_schedules s JOIN buckets b ON b.bucket_id = s.bucket_id"
                        + " WHERE s.schedule_id = :id AND s.deleted = false",
                new MapSqlParameterSource("id", id));
        if (rows.isEmpty())
            return null;
        Map<String, Object> row = rows.get(0);
        List<Object> rules = null;
        Object text = row.get("rules");
        if (text != null)
            try {
                rules = objectMapper.readValue(text.toString(), new TypeReference<>() {
                });
            } catch (Exception e) {
                rules = null;
            }
        return new Schedule(id, ((Number) row.get("project_id")).intValue(), (String) row.get("schedule_name"),
                ((Number) row.get("task_id")).longValue(), (String) row.get("bucket_name"), rules,
                (String) row.get("cron"), (String) row.get("time_zone"), (String) row.get("run_as"));
    }

    Outcome run(Schedule schedule, String trigger) {
        Instant started = Instant.now();
        Outcome outcome;
        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            SecurityContextHolder.setContext(contextOf(schedule));
            outcome = execute(schedule);
        } catch (Exception e) {
            outcome = new Outcome(ERROR, null, message(e));
        } finally {
            SecurityContextHolder.setContext(previous);
        }
        try {
            record(schedule, trigger, started, outcome);
        } catch (RuntimeException e) {
            logger.warn("Failed to record the run of the schedule {}: {}", schedule.id(), e.getMessage());
        }
        AuditLog.log("task.schedule-run", "id={} name={} trigger={} status={} count={}", schedule.id(), schedule.name(),
                trigger, outcome.status(), outcome.count());
        return outcome;
    }

    // the user the schedule runs as, signed in to the schedule's project: refused as a sign-in would be (user
    // disabled or expired, no longer in the project, project disabled or expired)
    private SecurityContext contextOf(Schedule schedule) throws Exception {
        CustomUserDetails details = userService.loadTokenUser(schedule.runAs(), schedule.projectId());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(details, "", details.getAuthorities()));
        return context;
    }

    private Outcome execute(Schedule schedule) throws Exception {
        TaskRunDTO rules = new TaskRunDTO();
        rules.setRules(schedule.rules());
        String taskRef = String.valueOf(schedule.taskId());
        TaskRunResponse preview = taskRunService.run(schedule.bucketName(), taskRef, rules, true);
        if (!"reserve".equals(preview.action()) && preview.count() > maxRows)
            return new Outcome(ERROR, null, "The task would run on " + preview.count() + " data rows, more than "
                    + maxRows + " (databucket.task-schedules.max-rows); narrow the rules");
        TaskRunResponse response = taskRunService.run(schedule.bucketName(), taskRef, rules, false);
        return new Outcome(OK, response.count(), response.message());
    }

    private void record(Schedule schedule, String trigger, Instant started, Outcome outcome) {
        long duration = Instant.now().toEpochMilli() - started.toEpochMilli();
        transactionTemplate.executeWithoutResult(status -> {
            MapSqlParameterSource params = new MapSqlParameterSource("id", schedule.id())
                    .addValue("p", schedule.projectId())
                    .addValue("started", Timestamp.from(started))
                    .addValue("duration", duration)
                    .addValue("trigger", trigger)
                    .addValue("runAs", schedule.runAs())
                    .addValue("status", outcome.status())
                    .addValue("count", outcome.count())
                    .addValue("message", truncate(outcome.message()))
                    .addValue("keep", historySize)
                    .addValue("maxFailures", maxFailures);
            jdbcTemplate.update("INSERT INTO task_schedule_runs"
                    + " (project_id, schedule_id, started_at, duration_ms, run_trigger, run_as, status, row_count, message)"
                    + " VALUES (:p, :id, :started, :duration, :trigger, :runAs, :status, :count, :message)", params);
            jdbcTemplate.update("DELETE FROM task_schedule_runs WHERE schedule_id = :id AND run_id NOT IN"
                    + " (SELECT run_id FROM task_schedule_runs WHERE schedule_id = :id ORDER BY run_id DESC LIMIT :keep)", params);

            if (SCHEDULE.equals(trigger) && ERROR.equals(outcome.status())) {
                // a failure of the scheduler counts; the schedule stops after maxFailures in a row
                jdbcTemplate.update("UPDATE task_schedules SET last_run_at = :started, last_status = :status,"
                        + " last_count = :count, last_message = :message, failures = failures + 1,"
                        + " enabled = (failures + 1 < :maxFailures),"
                        + " next_run_at = CASE WHEN failures + 1 < :maxFailures THEN next_run_at END"
                        + " WHERE schedule_id = :id", params);
                Integer failures = jdbcTemplate.queryForObject(
                        "SELECT failures FROM task_schedules WHERE schedule_id = :id", params, Integer.class);
                if (failures != null && failures >= maxFailures) {
                    jdbcTemplate.update("UPDATE task_schedules SET last_message = :message WHERE schedule_id = :id",
                            new MapSqlParameterSource("id", schedule.id()).addValue("message", truncate(
                                    "Disabled after " + failures + " failed runs in a row. " + outcome.message())));
                    AuditLog.log("task.schedule-disabled", "id={} name={} failures={}", schedule.id(), schedule.name(), failures);
                }
            } else {
                // a run that worked resets the count; "Run now" leaves the count of the scheduler's failures as it is
                jdbcTemplate.update("UPDATE task_schedules SET last_run_at = :started, last_status = :status,"
                        + " last_count = :count, last_message = :message"
                        + (OK.equals(outcome.status()) ? ", failures = 0" : "")
                        + " WHERE schedule_id = :id", params);
            }
        });
    }

    static String message(Exception e) {
        if (e instanceof DataIntegrityViolationException d) {
            String text = String.valueOf(d.getMessage());
            if (text.contains("is not present in table \"tags\""))
                return "The tag set by the task doesn't exist";
            if (text.contains("cannot cast jsonb null"))
                return "A property in the rules is empty and can't be compared";
            return "The data could not be saved: " + d.getMostSpecificCause().getMessage();
        }
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private static String truncate(String text) {
        return text == null || text.length() <= MESSAGE_MAX ? text : text.substring(0, MESSAGE_MAX - 1) + "…";
    }
}
