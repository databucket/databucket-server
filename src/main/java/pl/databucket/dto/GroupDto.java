package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupDto {
    private Long id;
    private String name;
    private String description;
    private Iterable<Long> buckets;
    private Iterable<Long> users;
}
