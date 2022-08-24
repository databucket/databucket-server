package pl.databucket.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
public class TemplateDataItemDto {

    private Long id;
    private int dataId; // templateData
    private String tagUid;
    private Boolean reserved;
    private String owner;
    private Map<String, Object> properties;
    private String createdBy;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date createdAt;
    private String modifiedBy;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date modifiedAt;
}
