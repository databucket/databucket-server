package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataColumnsItemDto {

    private String title;
    private String field;
    private String type;
    private boolean enabled;
    private String align;
    private boolean hidden;
    private String format;
    private String width;
    private Integer enumId;
    private String editable;
    private boolean sorting;
    private boolean filtering;
}
