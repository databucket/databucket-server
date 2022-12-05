package pl.databucket.server.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "projects")
public class Project extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_generator")
    @SequenceGenerator(name = "project_generator", sequenceName = "project_seq", allocationSize = 1)
    @Column(name = "project_id", updatable = false, nullable = false)
    private int id;

    @Column(name = "project_name", nullable = false, length = Constants.NAME_MAX)
    private String name;

    @Column(length = Constants.DESCRIPTION_MAX)
    private String description = null;

    @Column(name = "expiration_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expirationDate;

    @Column
    private Boolean publicVisible = false;

    @Column
    private Boolean enabled = true;

    @Column
    private Boolean deleted = false;

    @ManyToMany(mappedBy = "projects")
    private Set<User> users;

    @ManyToMany(mappedBy = "projects")
    private Set<Template> templates;

    public Set<Long> getUsersIds() {
        if (users != null && users.size() > 0)
            return users.stream().map(User::getId).collect(Collectors.toSet());
        else
            return null;
    }

    public Set<Integer> getTemplatesIds() {
        if (templates != null && templates.size() > 0)
            return templates.stream().map(Template::getId).collect(Collectors.toSet());
        else
            return null;
    }

    public boolean isExpired() {
        return (expirationDate != null && expirationDate.before(new Date()));
    }
}

