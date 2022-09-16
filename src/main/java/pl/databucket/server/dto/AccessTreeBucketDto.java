package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccessTreeBucketDto {

    private Long id;
    private String name;
    private String iconName;
    private String iconColor;
    private String iconSvg;
    private Long classId;

}
