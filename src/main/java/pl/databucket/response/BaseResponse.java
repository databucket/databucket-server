package pl.databucket.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"message", "page", "limit", "total", "total_pages", "sort"})
public class BaseResponse {

	private String message = null;
	private Integer page = null;
	private Integer limit = null;
	private Long total = null;
	@JsonProperty("total_pages")
	private Integer totalPages = null;
	private String sort = null;

}
