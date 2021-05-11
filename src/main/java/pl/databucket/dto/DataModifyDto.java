package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.service.data.Condition;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DataModifyDto {

    private Long tagId;
    private Boolean reserved;
    private Map<String, Object> properties;
    private Map<String, Object> propertiesToSet;
    private List<String> propertiesToRemove;
    private List<Map<String, Object>> conditions;
//    private List<Rule> rules; //TODO implement new approach for filtering

}
