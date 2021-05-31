package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DataFilterConfigDto {

    private List<Map<String, Object>> properties;
    private Map<String, Object> logic;
    private Map<String, Object> tree;

}
