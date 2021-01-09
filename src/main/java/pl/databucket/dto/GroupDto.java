package pl.databucket.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupDto {
    private Long id;
    private String name;
    private String description;
    private Iterable<Long> buckets;
    @JsonIgnore
    private Iterable<Long> users;
}
