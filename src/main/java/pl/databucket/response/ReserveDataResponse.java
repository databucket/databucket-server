package pl.databucket.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.databucket.dto.DataDTO;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReserveDataResponse {

    @ApiModelProperty(position = 1, example = "1")
    private Integer limit;

    @ApiModelProperty(position = 2, example = "id")
    private String sort;

    @ApiModelProperty(position = 3, example = "1")
    private int reserved;

    @ApiModelProperty(position = 4, example = "10")
    private long available;

    @ApiModelProperty(hidden = true)
    private String message;

    @ApiModelProperty(position = 6)
    private List<DataDTO> data;
}
