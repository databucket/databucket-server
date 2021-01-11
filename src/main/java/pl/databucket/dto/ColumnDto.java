package pl.databucket.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class ColumnDto {

    private String title;
    private String field;
    private String type;
    private String editable;
    private boolean sorting;
    private boolean filtering;
}
