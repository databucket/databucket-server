package pl.databucket.server.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.databucket.server.dto.DataHistoryEntryDTO;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"page", "limit", "sort", "total", "totalPages", "history"})
@Schema(description = "One page of history entries")
public class GetDataHistoryResponse {

    @Schema(example = "1")
    private Integer page;

    @Schema(example = "20")
    private Integer limit;

    @Schema(example = "desc(id)")
    private String sort;

    @Schema(description = "Number of all matching history entries", example = "57")
    private Long total;

    @Schema(example = "3")
    private Integer totalPages;

    private List<DataHistoryEntryDTO> history;
}
