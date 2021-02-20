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

    @Column(name = "private", nullable = false)
    private boolean privateItem = false;

    @Column(name = "protected_data", nullable = false)
    private boolean protectedData = false;

    @ManyToMany(mappedBy = "buckets")
    private Set<User> users;

    @ManyToMany(mappedBy = "buckets")
    private Set<Group> groups;

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

}
