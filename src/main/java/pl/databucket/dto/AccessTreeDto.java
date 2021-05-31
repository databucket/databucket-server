package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AccessTreeDto {

    private List<AuthProjectDto> projects;
    private List<AccessTreeGroupDto> groups;
    private List<AccessTreeBucketDto> buckets;
    private List<AccessTreeViewDto> views;

}
