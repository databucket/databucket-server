package pl.databucket.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Table(name="users")
public class User extends Auditable<String> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_generator")
    @SequenceGenerator(name="user_generator", sequenceName = "user_seq", allocationSize = 1)
    @Column(name = "user_id", updatable = false, nullable = false)
    private long id;

    @Column(name = "user_name", length = 50, unique = true)
    private String name;

    @Column
    private String password;

    @Column(name = "change_password")
    private boolean changePassword = true;

    @Column
    private Boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = { @JoinColumn(name = "user_id") },
            inverseJoinColumns = { @JoinColumn(name = "role_id") })
    private Set<Role> roles;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_projects",
            joinColumns = { @JoinColumn(name = "user_id") },
            inverseJoinColumns = { @JoinColumn(name = "project_id") })
    private Set<Project> projects;

    @ManyToMany
    @JoinTable(name = "user_groups",
            joinColumns = { @JoinColumn(name = "user_id") },
            inverseJoinColumns = { @JoinColumn(name = "group_id") })
    private Set<Group> groups;

    @ManyToMany
    @JoinTable(name = "user_buckets",
            joinColumns = { @JoinColumn(name = "user_id") },
            inverseJoinColumns = { @JoinColumn(name = "bucket_id") })
    private Set<Bucket> buckets;

    public Set<Long> getRolesIds() {
       return roles.stream().map(Role::getId).collect(Collectors.toSet());
    }

    public Set<Integer> getProjectsIds() {
        return projects.stream().map(Project::getId).collect(Collectors.toSet());
    }

    public Set<Long> getGroupsIds() {
        return groups.stream().map(Group::getId).collect(Collectors.toSet());
    }

    public Set<Long> getBucketsIds() {
        return buckets.stream().map(Bucket::getId).collect(Collectors.toSet());
    }

}
