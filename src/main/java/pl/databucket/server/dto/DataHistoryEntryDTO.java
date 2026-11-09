package pl.databucket.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "dataId", "tagId", "reserved", "propertiesChanged", "properties", "modifiedAt", "modifiedBy"})
@Schema(description = "One change of a data row; only the items that changed are set")
public class DataHistoryEntryDTO {

    @Schema(description = "Id of the history entry", example = "120")
    private long id;

    @Schema(description = "Id of the data row", example = "15")
    private long dataId;

    @Schema(description = "The new tag, when it changed", example = "3")
    private Long tagId;

    @Schema(description = "The new reservation state, when it changed", example = "true")
    private Boolean reserved;

    @Schema(description = "Whether the properties changed")
    private boolean propertiesChanged;

    @Schema(description = "The properties of this version (only with properties=true and when they changed)")
    private Map<String, Object> properties;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date modifiedAt;

    @Schema(example = "tests-robot")
    private String modifiedBy;
}
