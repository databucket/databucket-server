package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter
@Setter
public class DataCreateDto {

    private Long tagId;
    private Boolean reserved;
    private Map<String, Object> properties;

}
