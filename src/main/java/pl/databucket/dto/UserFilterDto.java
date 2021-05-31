package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFilterDto {

    private Long id;
    private DataFilterConfigDto configuration;
}
