package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * The past of the data of a dataset: the rows are created {@code since} before the import (counted from the start
 * of its day, so the hours of the changes are kept) (an ISO-8601 duration,
 * e.g. "P30D"), then the changes are made in the order of their time, each one at its time before the import.
 * The changes are made like edits in the application, so the history of the buckets (where it is on) shows them,
 * by the users of the dataset.
 */
@Getter
@Setter
public class DatasetHistoryDto {

    private String since;
    private List<Change> changes = new ArrayList<>();

    @Getter
    @Setter
    public static class Change {
        // the bucket, by name, and the row, by its position in the bucket's data (from 0)
        private String bucket;
        private Integer row;
        // how long before the start of the day of the import, an ISO-8601 duration (e.g. "P12DT3H25M"), at most since
        private String ago;
        // a user of the dataset; none = the importing user
        private String user;
        // a tag of the dataset, 0 removes the tag; null leaves it
        private Long tagId;
        // true reserves the row for the user, false releases it; null leaves it
        private Boolean reserved;
        // merged into the properties of the row (nested objects are merged too); null leaves them
        private java.util.Map<String, Object> properties;
    }
}
