package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserColumnsDto {
    private Long id;
    private DataColumnsConfigDto configuration;
}
