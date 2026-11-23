package pl.databucket.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import pl.databucket.server.configuration.Constants;

import java.time.Instant;

/** One run of a job (by cron, Run or its keyword); written by TaskScheduleRunner with plain SQL. */
@Getter
@Setter
@Entity
@Table(name = "task_schedule_runs", indexes = @Index(name = "task_schedule_runs_schedule", columnList = "schedule_id, run_id"))
public class TaskScheduleRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id", updatable = false, nullable = false)
    private long id;

    @TenantId
    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "schedule_id", nullable = false)
    private long scheduleId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    // SCHEDULE (cron), MANUAL (Run) or KEYWORD
    @Column(name = "run_trigger", nullable = false, length = 10)
    private String trigger;

    // who it ran as: the job's runAs for the cron, else the caller
    @Column(name = "run_as", length = Constants.USERNAME_MAX)
    private String runAs;

    // OK or ERROR
    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "row_count")
    private Long count;

    @Column(length = 1000)
    private String message;
}
