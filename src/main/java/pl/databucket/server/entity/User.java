package pl.databucket.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

import jakarta.validation.constraints.Email;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User extends Auditable<String> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_generator")
    @SequenceGenerator(name = "user_generator", sequenceName = "user_seq", allocationSize = 1)
    @Column(name = "user_id", updatable = false, nullable = false)
    private long id;

    @Column(name = "user_name", length = Constants.USERNAME_MAX, unique = true)
    private String username;

    @Column(length = Constants.DESCRIPTION_MAX)
    private String description;

    @Email
    @Column(unique = true)
    private String email;

    @Column
    private String password;

    @Column(name = "change_password")
    private boolean changePassword = true;

    @Column
    private Boolean enabled = true;

    @Column(name = "expiration_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expirationDate;

    @Column(name = "last_send_email_forgot_password_link_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSendEmailForgotPasswordLinkDate;

    @Column(name = "last_send_email_temp_password_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastSendEmailTempPasswordDate;

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

    // emails are unique case-insensitively, so they are stored lower-cased and a blank one as null
    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank())
            return null;
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public void setEmail(String email) {
        this.email = normalizeEmail(email);
    }

    /** The id of the user's role (one role per user; of older accounts with several, the highest). */
    public Set<Short> getRolesIds() {
        Role top = getTopRole();
        return top != null ? new HashSet<>(Set.of(top.getId())) : null;
    }

    /**
     * The user's role. Roles form a hierarchy (see {@link Constants#ROLES_BY_LEVEL}); accounts from before 3.12.0
     * may still have several roles, then the highest counts.
     */
    public Role getTopRole() {
        if (roles == null)
            return null;
        return roles.stream()
                .filter(role -> Constants.roleLevel(role.getName()) >= 0)
                .max(Comparator.comparingInt(role -> Constants.roleLevel(role.getName())))
                .orElse(null);
    }

    /** Whether the user's role is the given one or higher; false for a null role. */
    public boolean hasRoleAtLeast(String roleName) {
        Role top = getTopRole();
        int needed = Constants.roleLevel(roleName);
        return top != null && needed >= 0 && Constants.roleLevel(top.getName()) >= needed;
    }

    public boolean hasRoleAtLeast(Role role) {
        return role != null && hasRoleAtLeast(role.getName());
    }

    /** The user's role and every role below it, e.g. ADMIN, MEMBER, ROBOT: the authorities of the user's tokens. */
    public List<String> getImpliedRoleNames() {
        Role top = getTopRole();
        if (top == null)
            return List.of();
        List<String> names = new ArrayList<>(Constants.ROLES_BY_LEVEL.subList(0, Constants.roleLevel(top.getName()) + 1));
        Collections.reverse(names);
        return names;
    }

    public Set<Integer> getProjectsIds() {
        if (projects != null && !projects.isEmpty())
            return projects.stream().map(Project::getId).collect(Collectors.toSet());
        else
            return null;
    }

    public Set<Short> getTeamsIds() {
        if (teams != null && !teams.isEmpty())
            return teams.stream().map(Team::getId).collect(Collectors.toSet());
        else
            return null;
    }

    public boolean isSuperUser() {
        return hasRoleAtLeast(Constants.ROLE_SUPER);
    }

    /** ADMIN or higher (SUPER). */
    public boolean isAdminUser() {
        return hasRoleAtLeast(Constants.ROLE_ADMIN);
    }

    public boolean isExpired() {
        return (expirationDate != null && expirationDate.before(new Date()));
    }

}
