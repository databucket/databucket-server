package pl.databucket.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.entity.TaskSchedule;

import java.util.List;
import java.util.Map;

/**
 * A job of a dataset (Settings > Jobs): its task and bucket by their ids in the dataset. Imported disabled: the cron
 * runs a job as the admin who saved it, so an admin of the new project enables it (and becomes the one it runs as).
 */
@Getter
@Setter
public class DatasetJobDto {

    @NotBlank
    @Size(min = Constants.NAME_MIN, max = Constants.NAME_MAX)
    private String name;
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;
    private Long taskId;
    private Long bucketId;
    // rules narrowing the filter of the task, in the "rules" format of the API
    private List<Object> rules;
    // the properties of the rules builder in the GUI
    private List<Map<String, Object>> properties;
    private String cron;
    private String timeZone;
    @Size(max = TaskSchedule.KEYWORD_MAX)
    private String keyword;

    public static DatasetJobDto of(TaskScheduleDto schedule) {
        DatasetJobDto job = new DatasetJobDto();
        job.setName(schedule.getName());
        job.setDescription(schedule.getDescription());
        job.setTaskId(schedule.getTaskId());
        job.setBucketId(schedule.getBucketId());
        job.setRules(schedule.getRules());
        job.setProperties(schedule.getProperties());
        job.setCron(schedule.getCron());
        job.setTimeZone(schedule.getTimeZone());
        job.setKeyword(schedule.getKeyword());
        return job;
    }
}
