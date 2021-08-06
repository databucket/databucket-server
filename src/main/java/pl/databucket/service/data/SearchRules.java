package pl.databucket.service.data;

import java.util.List;
import java.util.Map;

public interface SearchRules {
    void setConditions(List<Map<String, Object>> conditions);
    List<Map<String, Object>> getConditions();
    Map<String, Object> getLogic();
    List<Object> getRules();
}
