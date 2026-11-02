package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * A bucket on the Overview page: what it is, how much data it holds and how much of it is free,
 * and how big its history is.
 */
@Getter
@Setter
public class OverviewBucketDto {

    private Long id;
    private String name;
    private String description;
    private String iconName;
    private String iconColor;
    private String iconSvg;

    private long rows;
    // not reserved by anyone
    private long free;
    private long reservedByMe;
    private long reservedByOthers;
    // the latest change of a row, null when the bucket is empty
    private Date lastModifiedAt;

    // whether the changes of the data are kept
    private boolean history;
    // entries of the history table; an estimate for big tables
    private long historyEntries;
    // bytes on disk, with indexes
    private long dataSize;
    private long historySize;
    // the history is big enough to be worth clearing (or limiting)
    private boolean historyLarge;
}
