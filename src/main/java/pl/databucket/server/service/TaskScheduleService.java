package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.TaskRunDTO;
import pl.databucket.server.dto.TaskScheduleDto;
import pl.databucket.server.entity.Bucket;
import pl.databucket.server.entity.Task;
import pl.databucket.server.entity.TaskSchedule;
import pl.databucket.server.entity.TaskScheduleRun;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.logging.AuditLog;
import pl.databucket.server.repository.BucketRepository;
import pl.databucket.server.repository.TaskRepository;
import pl.databucket.server.repository.TaskScheduleRepository;
import pl.databucket.server.repository.TaskScheduleRunRepository;

import java.time.Instant;
import java.util.List;

/**
 * The task schedules of the project (Settings > Schedules). Saving one checks it the way it will run: the task must be
 * one of the bucket's tasks, the user who saves it must have access to the bucket and the role the task needs, and
 * the task's filter or the rules must select rows (a preview of the task is made). The schedule then runs as that
 * user.
 */
@Service
public class TaskScheduleService {

    private final TaskScheduleRepository scheduleRepository;
    private final TaskScheduleRunRepository runRepository;
    private final TaskRepository taskRepository;
    private final BucketRepository bucketRepository;
    private final TaskRunService taskRunService;
    private final TaskScheduleRunner runner;
    private final UserService userService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final int historySize;

    public TaskScheduleService(TaskScheduleRepository scheduleRepository, TaskScheduleRunRepository runRepository,
                               TaskRepository taskRepository, BucketRepository bucketRepository,
                               TaskRunService taskRunService, TaskScheduleRunner runner, UserService userService,
                               NamedParameterJdbcTemplate jdbcTemplate,
                               @Value("${databucket.task-schedules.history-size:100}") int historySize) {
        this.scheduleRepository = scheduleRepository;
        this.runRepository = runRepository;
        this.taskRepository = taskRepository;
        this.bucketRepository = bucketRepository;
        this.taskRunService = taskRunService;
        this.runner = runner;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
        this.historySize = historySize;
    }

    /** The schedule can't be saved as it is; the message says why. */
    public static class InvalidScheduleException extends Exception {
        public InvalidScheduleException(String message) {
            super(message);
        }
    }

    public List<TaskScheduleDto> getSchedules() {
        return scheduleRepository.findAllByDeletedOrderById(false).stream().map(TaskScheduleService::toDto).toList();
    }

    public TaskScheduleDto createSchedule(TaskScheduleDto dto) throws Exception {
        TaskSchedule schedule = new TaskSchedule();
        apply(schedule, dto);
        TaskSchedule saved = scheduleRepository.save(schedule);
        AuditLog.log("task.schedule-create", "id={} name={} task={} bucket={} cron={}", saved.getId(), saved.getName(),
                saved.getTask().getName(), saved.getBucket().getName(), saved.getCron());
        return toDto(saved);
    }

    public TaskScheduleDto modifySchedule(TaskScheduleDto dto) throws Exception {
        if (dto.getId() == null)
            throw new InvalidScheduleException("The id of the schedule is missing");
        TaskSchedule schedule = find(dto.getId());
        apply(schedule, dto);
        TaskSchedule saved = scheduleRepository.save(schedule);
        AuditLog.log("task.schedule-modify", "id={} name={} task={} bucket={} cron={} enabled={}", saved.getId(),
                saved.getName(), saved.getTask().getName(), saved.getBucket().getName(), saved.getCron(), saved.isEnabled());
        return toDto(saved);
    }

    public void deleteSchedule(long id) throws ItemNotFoundException {
        TaskSchedule schedule = find(id);
        schedule.setDeleted(true);
        schedule.setEnabled(false);
        schedule.setNextRunAt(null);
        scheduleRepository.save(schedule);
        jdbcTemplate.update("DELETE FROM task_schedule_runs WHERE schedule_id = :id", new MapSqlParameterSource("id", id));
        AuditLog.log("task.schedule-delete", "id={} name={}", id, schedule.getName());
    }

    /** The latest runs of the schedule, the newest first. */
    public List<TaskScheduleRun> getRuns(long id) throws ItemNotFoundException {
        find(id);
        return runRepository.findAllByScheduleIdOrderByIdDesc(id, PageRequest.of(0, historySize));
    }

    /** Runs the schedule now, as its run-as user; returns the schedule with the result of the run. */
    public TaskScheduleDto runNow(long id) throws ItemNotFoundException {
        find(id);
        runner.runNow(id);
        return toDto(find(id));
    }

    // a schedule of the current project (the repository is restricted to it)
    private TaskSchedule find(long id) throws ItemNotFoundException {
        TaskSchedule schedule = scheduleRepository.findByIdAndDeleted(id, false);
        if (schedule == null)
            throw new ItemNotFoundException(TaskSchedule.class, id);
        return schedule;
    }

    private void apply(TaskSchedule schedule, TaskScheduleDto dto) throws Exception {
        if (dto.getName() == null || dto.getName().isBlank())
            throw new InvalidScheduleException("The name is missing");
        if (dto.getTaskId() == null)
            throw new InvalidScheduleException("Choose the task");
        if (dto.getBucketId() == null)
            throw new InvalidScheduleException("Choose the bucket");
        Task task = taskRepository.findByIdAndDeleted(dto.getTaskId(), false);
        if (task == null)
            throw new ItemNotFoundException(Task.class, dto.getTaskId());
        Bucket bucket = bucketRepository.findByIdAndDeleted(dto.getBucketId(), false);
        if (bucket == null)
            throw new ItemNotFoundException(Bucket.class, dto.getBucketId());

        String cron = ScheduleCron.normalize(dto.getCron());
        String timeZone = ScheduleCron.zone(dto.getTimeZone()).getId();
        List<Object> rules = dto.getRules() == null || dto.getRules().isEmpty() ? null : dto.getRules();

        // as it will run: the task of the bucket, the access and the role of the user, a filter or rules, the rules
        // valid (exceptions of the task run, with their messages)
        TaskRunDTO runRules = new TaskRunDTO();
        runRules.setRules(rules);
        taskRunService.run(bucket.getName(), String.valueOf(task.getId()), runRules, true);

        schedule.setName(dto.getName().trim());
        schedule.setDescription(dto.getDescription());
        schedule.setTask(task);
        schedule.setBucket(bucket);
        schedule.setRules(rules);
        schedule.setCron(cron);
        schedule.setTimeZone(timeZone);
        schedule.setRunAs(userService.getCurrentUser().getUsername());
        boolean wasEnabled = schedule.isEnabled();
        schedule.setEnabled(dto.isEnabled());
        if (dto.isEnabled() && !wasEnabled)
            schedule.setFailures(0); // enabled again (also after it stopped on failures)
        schedule.setNextRunAt(dto.isEnabled() ? ScheduleCron.next(cron, timeZone, Instant.now()) : null);
    }

    static TaskScheduleDto toDto(TaskSchedule schedule) {
        TaskScheduleDto dto = new TaskScheduleDto();
        dto.setId(schedule.getId());
        dto.setName(schedule.getName());
        dto.setDescription(schedule.getDescription());
        dto.setTaskId(schedule.getTask() != null ? schedule.getTask().getId() : null);
        dto.setBucketId(schedule.getBucket() != null ? schedule.getBucket().getId() : null);
        dto.setRules(schedule.getRules());
        dto.setCron(schedule.getCron());
        dto.setTimeZone(schedule.getTimeZone());
        dto.setEnabled(schedule.isEnabled());
        dto.setRunAs(schedule.getRunAs());
        dto.setNextRunAt(schedule.getNextRunAt());
        dto.setLastRunAt(schedule.getLastRunAt());
        dto.setLastStatus(schedule.getLastStatus());
        dto.setLastCount(schedule.getLastCount());
        dto.setLastMessage(schedule.getLastMessage());
        dto.setFailures(schedule.getFailures());
        dto.setCreatedBy(schedule.getCreatedBy());
        dto.setCreatedAt(schedule.getCreatedAt());
        dto.setModifiedBy(schedule.getModifiedBy());
        dto.setModifiedAt(schedule.getModifiedAt());
        return dto;
    }
}
