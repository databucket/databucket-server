package pl.databucket.server.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.databucket.server.dto.DataDTO;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReserveDataResponse {

    @Schema(example = "1")
    private Integer limit;

    @Schema(example = "id")
    private String sort;

    @Schema(example = "1")
    private int reserved;

    @Schema(example = "10")
    private long available;

    @Schema(accessMode = AccessMode.READ_ONLY)
    private String message;

    private List<DataDTO> data;
}
