package pl.databucket.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DataResponse {
    private long page;
    private long limit;
    private long total;
    private long totalPages;
    private String message;
    private String sort;
    private Integer[] dataIds;
    private Integer dataId;
    private List<Map<String, Object>> data;
    private List<Map<String, Object>> history;
}
