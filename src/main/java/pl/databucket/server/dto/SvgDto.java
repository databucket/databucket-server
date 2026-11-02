package pl.databucket.server.dto;

import lombok.EqualsAndHashCode;
import pl.databucket.server.dto.validation.SafeSvg;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@EqualsAndHashCode
@Getter
@Setter
public class SvgDto {

    private Long id;
    private String name;
    @SafeSvg
    private String structure;

    private String createdBy;
    private Date createdAt;
    private String modifiedBy;
    private Date modifiedAt;

}
