package pl.databucket.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Date;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataDTO {

    @Schema(example = "15")
    private long id;
    @Schema(example = "1")
    private Long tagId;
    @Schema(example = "false")
    private boolean reserved;
    @Schema(example = "null")
    private String owner;
    @Schema(example = "{\"name\": \"John\", \"age\": 34}")
    private Map<String, Object> properties;
    @Schema(example = "user")
    private String createdBy;
    @Schema(example = "2021-05-22T14:30:24.011Z")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createdAt;

    @Schema(example = "user")
    private String modifiedBy;

    @Schema(example = "2021-05-22T14:30:24.011Z")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date modifiedAt;
}
