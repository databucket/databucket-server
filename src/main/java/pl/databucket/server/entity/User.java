package pl.databucket.server.entity;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

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
    @JoinTable(name = "users_roles",
            joinColumns = {@JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "role_id")})
    private Set<Role> roles;

    @ManyToMany
    @JoinTable(name = "users_projects",
            joinColumns = {@JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "project_id")})
    private Set<Project> projects;

    @ManyToMany
    @JoinTable(name = "users_teams",
            joinColumns = {@JoinColumn(name = "user_id")},
            inverseJoinColumns = {@JoinColumn(name = "team_id")})
    private Set<Team> teams;

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

    public Set<Short> getTeamsIds() {
        if (teams != null && teams.size() > 0)
            return teams.stream().map(Team::getId).collect(Collectors.toSet());
        else
            return null;
    }

    public boolean isSuperUser() {
        if (roles != null && roles.size() > 0)
            return roles.stream().anyMatch(role -> role.getName().equals(Constants.ROLE_SUPER));
        else
            return false;
    }

    public boolean isAdminUser() {
        if (roles != null && roles.size() > 0)
            return roles.stream().anyMatch(role -> role.getName().equals(Constants.ROLE_ADMIN));
        else
            return false;
    }

    public boolean isExpired() {
        return (expirationDate != null && expirationDate.before(new Date()));
    }

}
