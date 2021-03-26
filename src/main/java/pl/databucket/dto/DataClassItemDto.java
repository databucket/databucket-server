package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataClassItemDto {

    private String title;
    private String path;
    private String type;
    private Integer enumId;
    private String uuid;

}
