package pl.databucket.server.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
@JsonPropertyOrder({"page", "limit", "sort", "total", "totalPages", "data", "customData", "message"})
@Schema(description = "Response object for data retrieval operations")
public class GetDataResponse {

    @Schema(description = "Current page number", example = "1")
    private Integer page;

    @Schema(description = "Number of items per page", example = "10")
    private Integer limit;

    @Schema(description = "Sort field", example = "id")
    private String sort;

    @Schema(description = "Total number of items", example = "50")
    private Long total;

    @Schema(description = "Total number of pages", example = "5")
    private Integer totalPages;

    @Schema(description = "List of data items")
    private List<DataDTO> data;

    @Schema(hidden = true, description = "Response message")
    private String message;

    @Schema(
            description = "Custom data with selected columns",
            example = "[{\"id\":53,\"name\":\"Sabra\"},{\"id\":58,\"name\":\"Shenika\"},{\"id\":117,\"name\":\"Shawna\"}]"
    )
    private Object customData;
}
