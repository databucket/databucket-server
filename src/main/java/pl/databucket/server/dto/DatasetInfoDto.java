package pl.databucket.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// a dataset as listed in the import dialog
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatasetInfoDto {

    // the file name without .json, e.g. "demo"
    private String id;
    private String name;
    private String description;
    private int buckets;
    private int views;
    private int rows;

}
