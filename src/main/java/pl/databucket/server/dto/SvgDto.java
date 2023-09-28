package pl.databucket.server.dto;

import java.util.Date;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SvgDto {

    Long id;
    String name;
    String structure;

    String createdBy;
    Date createdAt;
    String modifiedBy;
    Date modifiedAt;

}
