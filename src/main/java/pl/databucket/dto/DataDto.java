package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class DataDto {

    private long id;
    private Long tagId;
    private boolean reserved;
    private String owner;
    private Map<String, Object> properties;

    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;
}
