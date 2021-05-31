package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.service.data.SearchRules;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DataModifyDto implements SearchRules {

    private Long tagId;
    private Boolean reserved;
    private Map<String, Object> properties;
    private Map<String, Object> propertiesToSet;
    private List<String> propertiesToRemove;

    // Search rules
    private List<Map<String, Object>> conditions; // Old filtering method
    private Map<String, Object> logic; // New rules defined in frontend (property is saved as prop.$*group*itemName)
    private Map<String, Object> rules; // New rules defined in code (property is saved as $.group.itemName)

}
