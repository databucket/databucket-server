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
public class DataResponse extends BaseResponse {

	private List<Map<String, Object>> data = null;
	private List<Map<String, Object>> history = null;
	@JsonProperty("data_ids")
	private Integer[] dataIds = null;
	@JsonProperty("data_id")
	private Integer dataId = null;

}
