package pl.databucket.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.IconDto;
import pl.databucket.server.dto.IndexedPropertyDto;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Entity
@Getter
@Setter
@Table(name="buckets")
public class Bucket extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bucket_generator")
    @SequenceGenerator(name="bucket_generator", sequenceName = "bucket_seq", allocationSize = 1)
    @Column(name = "bucket_id", updatable = false, nullable = false)
    private long id;

    @TenantId
    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "bucket_name", length = Constants.NAME_MAX)
    private String name;

    @Column(name = "icon_name", length = Constants.NAME_MAX)
    private String iconName;

    @Column(name = "icon_color")
    private String iconColor;

    @Column(name = "icon_svg", columnDefinition="text")
    private String iconSvg;

    @Column(length = Constants.DESCRIPTION_MAX)
    private String description;

    @ManyToOne
    @JoinColumn(name = "class_id", referencedColumnName = "class_id")
    private DataClass dataClass;

    @Column(nullable = false)
    private boolean history = false;

    @Column(name = "protected_data", nullable = false)
    private boolean protectedData = false;

    // listed on the Overview page also for users without access, who can ask for it (the column default sets
    // it for the buckets that existed before)
    @Column(name = "overview_visible", nullable = false, columnDefinition = "boolean default true")
    private boolean overviewVisible = true;

    // how many of the latest history entries of each data row are kept (the entry of its creation always stays);
    // null: all; older entries are removed every night (HistoryCleanupService)
    @Column(name = "history_limit")
    private Integer historyLimit;

    // properties with a database index on the data table (BucketIndexService); null or empty: none
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "indexed_properties", columnDefinition = "jsonb")
    private List<IndexedPropertyDto> indexedProperties;

    @ManyToMany(mappedBy = "buckets")
    private Set<Group> groups;

    @ManyToMany(mappedBy = "buckets")
    private Set<View> views;

    @ManyToMany(mappedBy = "buckets")
    private Set<Tag> tags;

    @ManyToMany
    @JoinTable(name = "buckets_teams",
            joinColumns = {@JoinColumn(name = "bucket_id")},
            inverseJoinColumns = {@JoinColumn(name = "team_id")})
    private Set<Team> teams;

    @ManyToMany
    @JoinTable(name = "buckets_users",
            joinColumns = {@JoinColumn(name = "bucket_id")},
            inverseJoinColumns = {@JoinColumn(name = "user_id")})
    private Set<User> users;

    @ManyToOne
    @JoinColumn(name = "role_id", referencedColumnName = "role_id")
    private Role role;

    @JsonIgnore
    private Boolean deleted = false;

    public Set<Long> getUsersIds() {
        if (users != null && users.size() > 0)
            return users.stream().map(User::getId).collect(Collectors.toSet());
        else
            return null;
    }

    public Set<Long> getGroupsIds() {
        if (groups != null && groups.size() > 0)
            return groups.stream().map(Group::getId).collect(Collectors.toSet());
        else
            return null;
    }

    public Set<Short> getTeamsIds() {
        if (teams != null && teams.size() > 0)
            return teams.stream().map(Team::getId).collect(Collectors.toSet());
        else
            return null;
    }

    public String getTableName() {
        return String.format("x-bucket-%d", id);
    }

    /** The indexed properties: path -> type (text or number). */
    public Map<String, String> getIndexedPaths() {
        Map<String, String> paths = new java.util.LinkedHashMap<>();
        if (indexedProperties != null)
            for (IndexedPropertyDto property : indexedProperties)
                paths.put(property.getPath(), property.getType());
        return paths;
    }

    public String getTableHistoryName() {
        return getTableName() + "-h";
    }

    public IconDto getIconDto() {
        IconDto iconDto = new IconDto();
        iconDto.setName(iconName);
        iconDto.setColor(iconColor);
        iconDto.setSvg(iconSvg);
        return iconDto;
    }

}
