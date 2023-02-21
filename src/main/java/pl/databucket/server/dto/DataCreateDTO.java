package pl.databucket.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataCreateDTO {

    @Schema(example = "1")
    private Long tagId;

    @Schema(example = "false")
    private Boolean reserved;

    @Schema(example = "username")
    private String owner;

    @Schema(example = "{\"name\": \"John\", \"age\": 34}")
    private Map<String, Object> properties;

}
