package pl.databucket.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.databucket.configuration.Constants;

import javax.persistence.*;
import java.util.Set;

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

    @Column
    private Boolean deleted = false;

    @ManyToMany(mappedBy = "projects")
    private Set<User> users;
}

