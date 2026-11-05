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
    // "built-in" (in the jar) or "server" (in the folder databucket.datasets-dir)
    private String source;
    private String name;
    private String description;
    private int buckets;
    private int views;
    private int rows;
    private int icons;
    // changes in the history of the data, and the users of the dataset (created on import if missing)
    private int changes;
    private java.util.List<String> users;

    public static DatasetInfoDto of(String id, String source, DatasetDto dataset) {
        int rows = dataset.getData().values().stream().mapToInt(java.util.List::size).sum();
        return new DatasetInfoDto(id, source, dataset.getName(), dataset.getDescription(),
                dataset.getBuckets().size(), dataset.getViews().size(), rows, dataset.getIcons().size(),
                dataset.getHistory() == null ? 0 : dataset.getHistory().getChanges().size(),
                dataset.getUsers().stream().map(DatasetUserDto::getUsername).toList());
    }

}
