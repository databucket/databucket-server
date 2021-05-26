package pl.databucket.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
@Getter
@Setter
public class DataClassItemDto {

    private String title;
    private String path;
    private String type;
    private Integer enumId;
    private String uuid;

}
