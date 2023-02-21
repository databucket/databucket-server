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
public class GetDataResponse {

    @Schema(example = "1")
    private Integer page;

    @Schema(example = "10")
    private Integer limit;

    @Schema(example = "id")
    private String sort;

    @Schema(example = "50")
    private Long total;

    @Schema(example = "5")
    private Integer totalPages;

    private List<DataDTO> data;

    @Schema(accessMode = AccessMode.READ_ONLY)
    private String message;

    @Schema(example = "[{\"id\":53,\"name\":\"Sabra\"},{\"id\":58,\"name\":\"Shenika\"},{\"id\":117,\"name\":\"Shawna\"}]")
    private Object customData;
}
