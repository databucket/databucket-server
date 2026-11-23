package pl.databucket.server.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class TaskScheduleDto {

    private Long id;
    @Size(min = Constants.NAME_MIN, max = Constants.NAME_MAX)
    private String name;
    @Size(max = Constants.DESCRIPTION_MAX)
    private String description;
    private Long taskId;
    private Long bucketId;
    private List<Object> rules;
    private String cron;
    @Size(max = pl.databucket.server.entity.TaskSchedule.KEYWORD_MAX)
    private String keyword;
    private String timeZone;
    private boolean enabled;

    // read only
    private String runAs;
    private Instant nextRunAt;
    private Instant lastRunAt;
    private String lastStatus;
    private Long lastCount;
    private String lastMessage;
    private int failures;

    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
}
