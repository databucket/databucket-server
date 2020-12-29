package pl.databucket.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventResponse extends BaseResponse {

	@JsonProperty("event_id")
	private Integer eventId = null;
	private List<Map<String, Object>> events = null;
	private List<Map<String, Object>> events_history = null;
	@JsonProperty("events_log")
	private List<Map<String, Object>> eventsLog = null;

}
