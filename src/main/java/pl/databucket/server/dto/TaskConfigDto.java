package pl.databucket.server.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

// equals: Hibernate compares the jsonb value with its snapshot to tell whether the entity changed; without it every
// entity loaded in a read-write transaction was saved again (and its modified by/at overwritten)
@EqualsAndHashCode
@Getter
@Setter
public class TaskConfigDto {

    private List<Map<String, Object>> properties;
    private Map<String, Object> actions;

}
