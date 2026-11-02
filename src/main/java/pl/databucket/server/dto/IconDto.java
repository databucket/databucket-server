package pl.databucket.server.dto;

import lombok.AllArgsConstructor;
import pl.databucket.server.dto.validation.SafeSvg;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IconDto {

    private String name;
    private String color;
    @SafeSvg
    private String svg;

}
