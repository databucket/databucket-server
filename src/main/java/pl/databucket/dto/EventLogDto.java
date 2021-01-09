package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.entity.Bucket;
import pl.databucket.entity.Event;
import pl.databucket.entity.Task;

@Getter
@Setter
public class EventLogDto {

	private long id;
    private Event event;
    private Task task;
    private Bucket bucket;
    private Integer affected;
	private Boolean deleted = false;
}

