package pl.databucket.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataResponse {
    private Integer page;
    private Integer limit;
    private Long total;
    private Integer totalPages;
    private String sort;
    private Object data;
}
