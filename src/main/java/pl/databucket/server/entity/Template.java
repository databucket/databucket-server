package pl.databucket.server.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.TemplateConfDto;

import javax.persistence.*;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Table(name="templates")
public class Template extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "template_generator")
    @SequenceGenerator(name="template_generator", sequenceName = "template_seq", allocationSize = 1)
    @Column(name = "template_id", updatable = false, nullable = false)
    private int id;

    @ManyToMany
    @JoinTable(name = "templates_projects",
            joinColumns = {@JoinColumn(name = "template_id")},
            inverseJoinColumns = {@JoinColumn(name = "project_id")})
    private Set<Project> projects;

    @Column(name = "template_name", length = Constants.NAME_MAX)
    private String name;

    @Column(length = Constants.DESCRIPTION_MAX)
    private String description;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(name = "templates_base",
            joinColumns = {@JoinColumn(name = "template_id")},
            inverseJoinColumns = {@JoinColumn(name = "base_template_id")})
    private Set<Template> templates;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private TemplateConfDto configuration;

    public Set<Integer> getProjectsIds() {
        if (projects != null && projects.size() > 0)
            return projects.stream().map(Project::getId).collect(Collectors.toSet());
        else
            return null;
    }

    public Set<Integer> getTemplatesIds() {
        if (templates != null && templates.size() > 0)
            return templates.stream().map(Template::getId).collect(Collectors.toSet());
        else
            return null;
    }

}
