package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class UserColumnsDto {
    private Long id;
    private DataColumnsConfigDto configuration;
}
