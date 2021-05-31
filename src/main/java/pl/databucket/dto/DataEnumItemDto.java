package pl.databucket.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
@Getter
@Setter
public class DataEnumItemDto {
    private String value;
    private String text;
    private String icon;
}
