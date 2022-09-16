package pl.databucket.server.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.tenant.TenantSupport;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "svg")
@Filter(name = "projectFilter", condition = "project_id = :projectId")
public class Svg extends Auditable<String> implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "svg_generator")
    @SequenceGenerator(name = "svg_generator", sequenceName = "svg_seq", allocationSize = 1)
    @Column(name = "svg_id", updatable = false, nullable = false)
    private long id;

    @Column(name = "svg_name", nullable = false, length = 200)
    private String name;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "svg_structure", nullable = false, columnDefinition="text")
    private String structure;
}

