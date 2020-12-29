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
public class GroupResponse extends BaseResponse {

	@JsonProperty("group_id")
	private Integer groupId = null;
	private List<Map<String, Object>> groups = null;

}
