package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import pl.databucket.configuration.Constants;
import pl.databucket.tenant.TenantSupport;


@Entity
@Getter
@Setter
@Table(name="buckets")
@FilterDef(name = "projectFilter", parameters=@ParamDef(name="projectId", type="int"))
@Filter(name = "projectFilter", condition = "project_id = :projectId")
public class Bucket extends Auditable<String> implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bucket_generator")
    @SequenceGenerator(name="bucket_generator", sequenceName = "bucket_seq", allocationSize = 1)
    @Column(name = "bucket_id", updatable = false, nullable = false)
    private long id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "bucket_name", length = Constants.NAME_MAX)
    private String name;

    @Column(name = "icon_name", length = Constants.NAME_MAX)
    private String iconName;

    @Column(length = Constants.DESCRIPTION_MAX)
    private String description;

    @OneToOne
    @JoinColumn(name = "class_id", referencedColumnName = "class_id")
    private DataClass dataClass;

    @Column(nullable = false)
    private boolean history = false;

    @Column(name = "protected_data", nullable = false)
    private boolean protectedData = false;

    @ManyToMany(mappedBy = "buckets")
    private Set<Group> groups;

    @ManyToMany(mappedBy = "buckets")
    private Set<View> views;

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

    @OneToOne
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

}
