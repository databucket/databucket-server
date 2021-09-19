package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TaskConfigDto {

    private List<Map<String, Object>> properties;
    private Map<String, Object> actions;

}
