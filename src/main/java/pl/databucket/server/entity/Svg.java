package pl.databucket.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "svg")
public class Svg extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "svg_generator")
    @SequenceGenerator(name = "svg_generator", sequenceName = "svg_seq", allocationSize = 1)
    @Column(name = "svg_id", updatable = false, nullable = false)
    private long id;

    @Column(name = "svg_name", nullable = false, length = 200)
    private String name;

    @TenantId
    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "svg_structure", nullable = false, columnDefinition="text")
    private String structure;
}

