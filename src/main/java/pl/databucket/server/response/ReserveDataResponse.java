package pl.databucket.server.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.databucket.server.dto.DataDTO;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response object for data reservation operations")
public class ReserveDataResponse {

    @Schema(description = "Maximum number of items to reserve", example = "1")
    private Integer limit;

    @Schema(description = "Sort field", example = "id")
    private String sort;

    @Schema(description = "Number of items reserved", example = "1")
    private int reserved;

    @Schema(description = "Number of available items", example = "10")
    private long available;

    @Schema(hidden = true, description = "Response message")
    private String message;

    @Schema(description = "List of reserved data items")
    private List<DataDTO> data;
}
