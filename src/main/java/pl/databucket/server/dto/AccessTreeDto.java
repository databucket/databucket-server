package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AccessTreeDto {

    private List<AuthProjectDTO> projects;
    private List<AccessTreeGroupDto> groups;
    private List<AccessTreeBucketDto> buckets;
    private List<AccessTreeViewDto> views;

}
