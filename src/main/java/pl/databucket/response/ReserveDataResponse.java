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

    @ApiModelProperty(position = 3)
    private List<DataDTO> data;
}
