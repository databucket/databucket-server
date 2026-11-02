package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

// a pending request for access, as the admins of the project see it on the Overview page
@Getter
@Setter
public class BucketAccessRequestDto {

    private Long id;
    private String bucketName;
    private String iconName;
    private String iconColor;
    private String iconSvg;
    private String username;
    private String email;
    private Date requestedAt;
}
