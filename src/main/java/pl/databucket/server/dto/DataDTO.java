package pl.databucket.server.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class DataDTO {

    @ApiModelProperty(position = 1, example = "15")
    private long id;

    @ApiModelProperty(position = 2, example = "1")
    private Long tagId;

    @ApiModelProperty(position = 3, example = "false")
    private boolean reserved;

    @ApiModelProperty(position = 4, example = "null")
    private String owner;

    @ApiModelProperty(position = 5, example = "{\"name\": \"John\", \"age\": 34}")
    private Map<String, Object> properties;

    @ApiModelProperty(position = 6, example = "user")
    private String createdBy;

    @ApiModelProperty(position = 7, example = "2021-05-22T14:30:24.011+0000")
    private Date createdAt;

    @ApiModelProperty(position = 8, example = "user")
    private String modifiedBy;

    @ApiModelProperty(position = 9, example = "2021-05-22T14:30:24.011+0000")
    private Date modifiedAt;
}
