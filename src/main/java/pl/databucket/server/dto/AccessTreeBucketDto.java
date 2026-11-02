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
    // whether the changes of its data are kept (the data history and clearing it are offered only then)
    private boolean history;

}
