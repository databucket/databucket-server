package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Schema(description = "Data transfer object for creating new data")
public class DataCreateDTO {

    @Schema(description = "Tag ID", example = "1")
    private Long tagId;

    @Schema(description = "True reserves the new row for the signed-in user", example = "false")
    private Boolean reserved;

    @Schema(description = "Not used: a reserved row is always owned by the signed-in user", example = "username")
    private String owner;

    @Schema(description = "Data properties", example = "{\"name\": \"John\", \"age\": 34}")
    private Map<String, Object> properties;

}
