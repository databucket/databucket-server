package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import pl.databucket.configuration.Constants;
import pl.databucket.tenant.TenantSupport;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name="data_classes")
@Filter(name = "projectFilter", condition = "project_id = :projectId")
public class DataClass extends Auditable<String> implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "class_generator")
    @SequenceGenerator(name="class_generator", sequenceName = "class_seq", allocationSize = 1)
    @Column(name = "class_id", updatable = false, nullable = false)
    private long id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "class_name", length = Constants.NAME_MAX, unique = true)
    private String name;

    @Column(length = Constants.DESCRIPTION_MAX)
    private String description;

    @JsonIgnore
    private Boolean deleted = false;
}
