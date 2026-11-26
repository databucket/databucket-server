package pl.databucket.server.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.DatasetJobDto;
import pl.databucket.server.dto.JobKeywordRunResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The jobs (task schedules) of the project (Settings > Jobs). Saving one checks it the way it will run: the task must
 * be one of the bucket's tasks, the user who saves it must have access to the bucket and the role the task needs, and
 * the task's filter or the rules must select rows (a preview of the task is made). The cron then runs it as that
 * user; Run and the keyword run it as the caller. A job with neither cron nor keyword runs only by Run.
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
    private final JobKeywordLock keywordLock;
    private final int historySize;

    public TaskScheduleService(TaskScheduleRepository scheduleRepository, TaskScheduleRunRepository runRepository,
                               TaskRepository taskRepository, BucketRepository bucketRepository,
                               TaskRunService taskRunService, TaskScheduleRunner runner, UserService userService,
                               NamedParameterJdbcTemplate jdbcTemplate, JobKeywordLock keywordLock,
                               @Value("${databucket.task-schedules.history-size:100}") int historySize) {
        this.scheduleRepository = scheduleRepository;
        this.runRepository = runRepository;
        this.taskRepository = taskRepository;
        this.bucketRepository = bucketRepository;
        this.taskRunService = taskRunService;
        this.runner = runner;
        this.userService = userService;
        this.jdbcTemplate = jdbcTemplate;
        this.keywordLock = keywordLock;
        this.historySize = historySize;
    }

    /** The job can't be saved or run as it is; the message says why. */
    public static class InvalidScheduleException extends Exception {
        public InvalidScheduleException(String message) {
            super(message);
        }
    }

    /** No job of the project has the keyword; the message lists the keywords there are. */
    public static class KeywordNotFoundException extends Exception {
        public KeywordNotFoundException(String message) {
            super(message);
        }
    }

    /** The jobs of the keyword are running (started by someone else or in another instance). */
    public static class KeywordRunningException extends Exception {
        public KeywordRunningException(String message) {
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
        AuditLog.log("task.schedule-create", "id={} name={} task={} bucket={} cron={} keyword={}", saved.getId(),
                saved.getName(), saved.getTask().getName(), saved.getBucket().getName(), saved.getCron(),
                saved.getKeyword());
        return toDto(saved);
    }

    public TaskScheduleDto modifySchedule(TaskScheduleDto dto) throws Exception {
        if (dto.getId() == null)
            throw new InvalidScheduleException("The id of the job is missing");
        TaskSchedule schedule = find(dto.getId());
        apply(schedule, dto);
        TaskSchedule saved = scheduleRepository.save(schedule);
        AuditLog.log("task.schedule-modify", "id={} name={} task={} bucket={} cron={} keyword={} enabled={}",
                saved.getId(), saved.getName(), saved.getTask().getName(), saved.getBucket().getName(), saved.getCron(),
                saved.getKeyword(), saved.isEnabled());
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

    /**
     * A job of a dataset, in the current project: saved as it is (no preview: the data of the project may not be
     * there yet) and disabled, run as the importing user until an admin saves it.
     */
    public TaskSchedule importJob(DatasetJobDto job) throws Exception {
        Task task = taskRepository.findByIdAndDeleted(job.getTaskId(), false);
        if (task == null)
            throw new ItemNotFoundException(Task.class, job.getTaskId());
        Bucket bucket = bucketRepository.findByIdAndDeleted(job.getBucketId(), false);
        if (bucket == null)
            throw new ItemNotFoundException(Bucket.class, job.getBucketId());
        String keyword = keyword(job.getKeyword());
        if (keyword != null && keyword.length() > TaskSchedule.KEYWORD_MAX)
            throw new InvalidScheduleException("The keyword is longer than " + TaskSchedule.KEYWORD_MAX + " characters");
        TaskSchedule schedule = new TaskSchedule();
        schedule.setName(job.getName().trim());
        schedule.setDescription(job.getDescription());
        schedule.setTask(task);
        schedule.setBucket(bucket);
        schedule.setRules(job.getRules() == null || job.getRules().isEmpty() ? null : job.getRules());
        schedule.setProperties(emptyAsNull(job.getProperties()));
        schedule.setCron(job.getCron() == null || job.getCron().isBlank() ? null : ScheduleCron.normalize(job.getCron()));
        schedule.setTimeZone(ScheduleCron.zone(job.getTimeZone()).getId());
        schedule.setKeyword(keyword);
        schedule.setRunAs(userService.getCurrentUser().getUsername());
        schedule.setEnabled(false);
        schedule.setNextRunAt(null);
        return scheduleRepository.save(schedule);
    }

    /** The latest runs of the schedule, the newest first. */
    public List<TaskScheduleRun> getRuns(long id) throws ItemNotFoundException {
        find(id);
        return runRepository.findAllByScheduleIdOrderByIdDesc(id, PageRequest.of(0, historySize));
    }

    /** Run: runs the job now, as the caller; returns the job with the result of the run. */
    public TaskScheduleDto runNow(long id) throws ItemNotFoundException {
        find(id);
        runner.runAsCaller(id, TaskScheduleRunner.MANUAL, userService.getCurrentUser().getUsername());
        return toDto(find(id));
    }

    /**
     * Runs the jobs of the keyword one after another, by id, as the caller; a failed job doesn't stop the others (each
     * run is in the job's history). With preview, only counts the rows each job would run on.
     */
    public JobKeywordRunResponse runKeyword(String keyword, boolean preview) throws Exception {
        String wanted = keyword(keyword);
        List<TaskSchedule> all = scheduleRepository.findAllByDeletedOrderById(false);
        if (wanted == null)
            throw new InvalidScheduleException("Give the keyword" + known(all));
        List<TaskSchedule> jobs = all.stream()
                .filter(job -> job.getKeyword() != null && job.getKeyword().equalsIgnoreCase(wanted))
                .toList();
        if (jobs.isEmpty())
            throw new KeywordNotFoundException("No job has the keyword '" + wanted + "'" + known(all));

        if (preview)
            return response(wanted, true, jobs, jobs.stream().map(job -> runner.preview(job.getId())).toList());

        String caller = userService.getCurrentUser().getUsername();
        List<TaskScheduleRunner.Outcome> outcomes = new ArrayList<>();
        try (JobKeywordLock.Held held = keywordLock.tryLock(jobs.get(0).getProjectId(), wanted)) {
            if (held == null)
                throw new KeywordRunningException("The jobs of the keyword '" + wanted + "' are running; try again "
                        + "when they are done");
            for (TaskSchedule job : jobs)
                outcomes.add(runner.runAsCaller(job.getId(), TaskScheduleRunner.KEYWORD, caller));
        }
        JobKeywordRunResponse response = response(wanted, false, jobs, outcomes);
        AuditLog.log("task.keyword-run", "keyword={} jobs={} ok={} errors={}", wanted, jobs.size(), response.ok(),
                response.errors());
        return response;
    }

    private static JobKeywordRunResponse response(String keyword, boolean preview, List<TaskSchedule> jobs,
                                                  List<TaskScheduleRunner.Outcome> outcomes) {
        List<JobKeywordRunResponse.Job> results = new ArrayList<>();
        int ok = 0;
        for (int i = 0; i < jobs.size(); i++) {
            TaskSchedule job = jobs.get(i);
            TaskScheduleRunner.Outcome outcome = outcomes.get(i);
            if (TaskScheduleRunner.OK.equals(outcome.status()))
                ok++;
            results.add(new JobKeywordRunResponse.Job(job.getId(), job.getName(), job.getTask().getName(),
                    job.getBucket().getName(), outcome.status(), outcome.count(), outcome.message()));
        }
        return new JobKeywordRunResponse(keyword, preview, ok, jobs.size() - ok, results);
    }

    /** The keywords of the jobs, for messages: "; the keywords: 'a', 'b'" or "; no job has a keyword". */
    private static String known(List<TaskSchedule> jobs) {
        Map<String, String> keywords = new TreeMap<>();
        for (TaskSchedule job : jobs)
            if (job.getKeyword() != null)
                keywords.putIfAbsent(job.getKeyword().toLowerCase(Locale.ROOT), job.getKeyword());
        if (keywords.isEmpty())
            return "; no job has a keyword (Settings > Jobs)";
        return "; the keywords: " + String.join(", ", keywords.values().stream().map(k -> "'" + k + "'").toList());
    }

    /** The keyword as stored and compared: trimmed, inner spaces as one; null if blank. */
    static String keyword(String keyword) {
        if (keyword == null)
            return null;
        String trimmed = keyword.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
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

        // no cron: not run by the cron (only by its keyword or Run)
        String cron = dto.getCron() == null || dto.getCron().isBlank() ? null : ScheduleCron.normalize(dto.getCron());
        String timeZone = ScheduleCron.zone(dto.getTimeZone()).getId();
        String keyword = keyword(dto.getKeyword());
        if (keyword != null && keyword.length() > TaskSchedule.KEYWORD_MAX)
            throw new InvalidScheduleException("The keyword is longer than " + TaskSchedule.KEYWORD_MAX + " characters");
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
        schedule.setProperties(emptyAsNull(dto.getProperties()));
        schedule.setCron(cron);
        schedule.setKeyword(keyword);
        schedule.setTimeZone(timeZone);
        schedule.setRunAs(userService.getCurrentUser().getUsername());
        boolean wasEnabled = schedule.isEnabled();
        schedule.setEnabled(dto.isEnabled());
        if (dto.isEnabled() && !wasEnabled)
            schedule.setFailures(0); // enabled again (also after it stopped on failures)
        schedule.setNextRunAt(dto.isEnabled() && cron != null ? ScheduleCron.next(cron, timeZone, Instant.now()) : null);
    }

    private static List<Map<String, Object>> emptyAsNull(List<Map<String, Object>> properties) {
        return properties == null || properties.isEmpty() ? null : properties;
    }

    static TaskScheduleDto toDto(TaskSchedule schedule) {
        TaskScheduleDto dto = new TaskScheduleDto();
        dto.setId(schedule.getId());
        dto.setName(schedule.getName());
        dto.setDescription(schedule.getDescription());
        dto.setTaskId(schedule.getTask() != null ? schedule.getTask().getId() : null);
        dto.setBucketId(schedule.getBucket() != null ? schedule.getBucket().getId() : null);
        dto.setRules(schedule.getRules());
        dto.setProperties(schedule.getProperties());
        dto.setCron(schedule.getCron());
        dto.setKeyword(schedule.getKeyword());
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
