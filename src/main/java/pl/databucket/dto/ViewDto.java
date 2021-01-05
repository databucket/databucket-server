package pl.databucket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ViewDto {
    private Long id;
    private String name;
    private String description;
    private Long dataClassId;
    private Long bucketId;
    private long columnsId;
    private Long filterId;
}
