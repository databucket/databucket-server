package pl.databucket.model.beans;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class GroupBean {
    private String name;
    private String description;
    private List<Integer> buckets;
}
