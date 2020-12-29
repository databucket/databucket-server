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
public class TaskResponse extends BaseResponse {

	@JsonProperty("task_id")
	private Integer taskId = null;
	private List<Map<String, Object>> tasks = null;

}
