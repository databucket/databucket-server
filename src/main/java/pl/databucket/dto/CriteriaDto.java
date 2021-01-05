package pl.databucket.dto;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.database.Operator;

@Getter
@Setter
public class CriteriaDto {

    private String leftSource;
    private Object leftValue;
    private Operator operator;
    private String rightSource;
    private Object rightValue;
}
