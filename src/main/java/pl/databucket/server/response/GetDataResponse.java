package pl.databucket.server.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.databucket.server.dto.DataDTO;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetDataResponse {

    @ApiModelProperty(position = 1, example = "1")
    private Integer page;

    @ApiModelProperty(position = 2, example = "10")
    private Integer limit;

    @ApiModelProperty(position = 3, example = "id")
    private String sort;

    @ApiModelProperty(position = 4, example = "50")
    private Long total;

    @ApiModelProperty(position = 5, example = "5")
    private Integer totalPages;

    @ApiModelProperty(position = 6)
    private List<DataDTO> data;

    @ApiModelProperty(hidden = true)
    private String message;

    @ApiModelProperty(position = 7, example = "[{\"id\":53,\"name\":\"Sabra\"},{\"id\":58,\"name\":\"Shenika\"},{\"id\":117,\"name\":\"Shawna\"}]")
    private Object customData;
}
