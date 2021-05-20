package pl.databucket.service.data;

import java.util.List;
import java.util.Map;

public interface SearchRules {

    void setConditions(List<Map<String, Object>> conditions);
    List<Map<String, Object>> getConditions();
    void setLogic(Map<String, Object> logic);
    Map<String, Object> getLogic();
    void setRules(Map<String, Object> rules);
    Map<String, Object> getRules();

}
