package pl.databucket.server.dto;

import lombok.AllArgsConstructor;
import pl.databucket.server.dto.validation.SafeSvg;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// equals: Hibernate compares the jsonb value with its snapshot to tell whether the entity changed; without it every
// entity loaded in a read-write transaction was saved again (and its modified by/at overwritten)
@EqualsAndHashCode
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
