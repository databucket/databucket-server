package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DataRemoveDto {

    private List<Map<String, Object>> conditions;
//    private List<Rule> rules; //TODO implement new approach for filtering

}
