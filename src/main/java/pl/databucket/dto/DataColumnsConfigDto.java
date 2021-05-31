package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DataColumnsConfigDto {

    private List<Map<String, Object>> columns;
    private List<Map<String, Object>> properties;
}
