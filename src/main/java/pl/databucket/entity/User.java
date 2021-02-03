package pl.databucket.entity;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.configuration.Constants;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User extends Auditable<String> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_generator")
    @SequenceGenerator(name = "user_generator", sequenceName = "user_seq", allocationSize = 1)
    @Column(name = "user_id", updatable = false, nullable = false)
    private long id;

    @Column(name = "user_name", length = Constants.NAME_MAX, unique = true)
    private String username;

    @Column
    private String password;

    @Column(name = "change_password")
    private boolean changePassword = true;

    @Column
    private Boolean enabled = true;

    @Column(name = "expiration_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expirationDate;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = {@JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "role_id")})
    private Set<Role> roles;

    @ManyToMany
    @JoinTable(name = "user_projects",
            joinColumns = {@JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "project_id")})
    private Set<Project> projects;

    @ManyToMany
    @JoinTable(name = "user_groups",
            joinColumns = {@JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "group_id")})
    private Set<Group> groups;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_buckets",
            joinColumns = {@JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "bucket_id")})
    private Set<Bucket> buckets;

    public Set<Short> getRolesIds() {
        if (roles != null && roles.size() > 0)
            return roles.stream().map(Role::getId).collect(Collectors.toSet());
        else
            return null;
    }

    public Set<Integer> getProjectsIds() {
        if (projects != null && projects.size() > 0)
            return projects.stream().map(Project::getId).collect(Collectors.toSet());
        else
            return null;
    }

    public Set<Long> getGroupsIds() {
        if (groups != null && groups.size() > 0)
            return groups.stream().map(Group::getId).collect(Collectors.toSet());
        else
            return null;
    }

    public Set<Long> getBucketsIds() {
        if (buckets != null && buckets.size() > 0)
            return buckets.stream().map(Bucket::getId).collect(Collectors.toSet());
        else
            return null;
    }

    public boolean isSuperUser() {
        if (roles != null && roles.size() > 0)
            return roles.stream().anyMatch(role -> role.getName().equals(Constants.ROLE_SUPER));
        else
            return false;
    }

    public boolean isExpired() {
        return (expirationDate != null && expirationDate.before(new Date()));
    }

}
