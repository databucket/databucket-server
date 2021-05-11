package pl.databucket.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DataResponse {
    private long page;
    private long limit;
    private long total;
    private long totalPages;
    private String sort;
    private Object data;
}
