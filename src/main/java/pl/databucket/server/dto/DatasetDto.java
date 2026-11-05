package pl.databucket.server.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A dataset: the content of a project (src/main/resources/datasets/*.json), imported into an empty project.
 * The objects keep the ids of the project they come from; they are only keys that link the objects (a bucket's
 * class, a view's columns, a filter's tags...), the import creates new objects and maps the ids. Access is given
 * by roles and teams. A dataset may name users (e.g. demo users) and a history of its data made by them.
 */
@Getter
@Setter
public class DatasetDto {

    // the version of the file format (DatasetCheck.FORMAT); a file without it is of the first one
    private Integer format;
    private String name;
    private String description;

    private List<TeamDto> teams = new ArrayList<>();
    private List<DataEnumDto> enums = new ArrayList<>();
    private List<DataClassDto> classes = new ArrayList<>();
    private List<GroupDto> groups = new ArrayList<>();
    private List<BucketDto> buckets = new ArrayList<>();
    private List<TagDto> tags = new ArrayList<>();
    private List<DataColumnsDto> columns = new ArrayList<>();
    private List<DataFilterDto> filters = new ArrayList<>();
    private List<ViewDto> views = new ArrayList<>();
    private List<TaskDto> tasks = new ArrayList<>();
    // the icons uploaded to the project (Settings > Icons); the objects keep a copy of the icon they use
    private List<SvgDto> icons = new ArrayList<>();
    // the users who made the history of the data (format 2); they get access to the project
    private List<DatasetUserDto> users = new ArrayList<>();
    // the history of the data (format 2): changes made after the rows were created, back in time; none = no history
    private DatasetHistoryDto history;

    // the data rows of each bucket, by the bucket's name; tagId is a tag of this dataset
    private Map<String, List<DataCreateDTO>> data = new LinkedHashMap<>();

}
