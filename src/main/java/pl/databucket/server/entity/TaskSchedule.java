package pl.databucket.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;
import pl.databucket.server.configuration.Constants;

import java.time.Instant;
import java.util.List;

/**
 * Runs a task on a bucket on a schedule (cron), as the user who saved it last (runAs). The due schedules of all
 * projects are picked by TaskScheduleRunner with plain SQL (SELECT ... FOR UPDATE SKIP LOCKED), so each run happens
 * once also with several instances; the last* fields and the runs are written there too.
 */
@Getter
@Setter
@Entity
@Table(name = "task_schedules")
public class TaskSchedule extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_schedule_generator")
    @SequenceGenerator(name = "task_schedule_generator", sequenceName = "task_schedule_seq", allocationSize = 1)
    @Column(name = "schedule_id", updatable = false, nullable = false)
    private long id;

    @TenantId
    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "schedule_name", length = Constants.NAME_MAX)
    private String name;

    @Column(length = Constants.DESCRIPTION_MAX)
    private String description;

    @ManyToOne
    @JoinColumn(name = "task_id", referencedColumnName = "task_id", nullable = false)
    private Task task;

    @ManyToOne
    @JoinColumn(name = "bucket_id", referencedColumnName = "bucket_id", nullable = false)
    private Bucket bucket;

    // rules narrowing the filter of the task, in the "rules" format of the API (may hold @now-2h)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Object> rules;

    // 6 fields (second minute hour day month weekday), as Spring's CronExpression
    @Column(nullable = false, length = 100)
    private String cron;

    @Column(name = "time_zone", nullable = false, length = 50)
    private String timeZone;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "run_as", nullable = false, length = Constants.USERNAME_MAX)
    private String runAs;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "last_status", length = 10)
    private String lastStatus;

    @Column(name = "last_count")
    private Long lastCount;

    @Column(name = "last_message", length = 1000)
    private String lastMessage;

    // consecutive failed runs; the schedule is disabled after databucket.task-schedules.max-failures
    @Column(nullable = false)
    private int failures;

    private Boolean deleted = false;
}
