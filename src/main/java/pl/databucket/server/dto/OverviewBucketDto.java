package pl.databucket.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * A bucket on the Overview page: what it is, how much data it holds and how much of it is free, and how big its
 * history is. For a bucket the user has no access to (listed because it is visible on the Overview page) only its
 * name, description, rows and free rows are given, with the user's request for access.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OverviewBucketDto {

    private Long id;
    private String name;
    private String description;
    private String iconName;
    private String iconColor;
    private String iconSvg;

    // whether the user has access to the data
    private boolean access;

    private long rows;
    // not reserved by anyone
    private long free;

    // with access only
    private Long reservedByMe;
    private Long reservedByOthers;
    // the latest change of a row, null when the bucket is empty
    private Date lastModifiedAt;
    // whether the changes of the data are kept
    private Boolean history;
    // the latest history entries kept for each data row (removed every night), null: all
    private Integer historyLimit;
    // entries of the history table; an estimate for big tables
    private Long historyEntries;
    // bytes on disk, with indexes
    private Long dataSize;
    private Long historySize;
    // the history is big enough to be worth clearing (or limiting)
    private Boolean historyLarge;

    // without access: the user's latest request for access (PENDING or REJECTED), if any
    private String requestStatus;
    private Date requestedAt;
}
