package pl.databucket.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
@Schema(description = "Data transfer object representing a data entity")
public class DataDTO {

    @Schema(description = "Data ID", example = "15")
    private long id;

    @Schema(description = "Tag ID", example = "1")
    private Long tagId;

    @Schema(description = "Reserved flag", example = "false")
    private boolean reserved;

    @Schema(description = "Owner username", example = "null", nullable = true)
    private String owner;

    @Schema(description = "Data properties", example = "{\"name\": \"John\", \"age\": 34}")
    private Map<String, Object> properties;

    @Schema(description = "Created by username", example = "user")
    private String createdBy;

    @Schema(description = "Creation timestamp", example = "2021-05-22T14:30:24.011Z")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createdAt;

    @Schema(description = "Modified by username", example = "user")
    private String modifiedBy;

    @Schema(description = "Modification timestamp", example = "2021-05-22T14:30:24.011Z")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date modifiedAt;
}
