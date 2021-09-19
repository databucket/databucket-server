package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFilterDto {

    private Long id;
    private DataFilterConfigDto configuration;
}
