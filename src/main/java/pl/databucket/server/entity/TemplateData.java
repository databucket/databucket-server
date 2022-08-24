package pl.databucket.server.entity;

import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;
import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name="templates_data")
public class TemplateData extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "data_generator")
    @SequenceGenerator(name="data_generator", sequenceName = "data_seq", allocationSize = 1)
    @Column(name = "data_id", updatable = false, nullable = false)
    private int id;

    @Column(name = "data_name", length = Constants.NAME_MAX)
    private String name;

    @Column(length = Constants.DESCRIPTION_MAX)
    private String description;

    @OneToOne
    @JoinColumn(name = "template_id", referencedColumnName = "template_id")
    private Template template;

}
