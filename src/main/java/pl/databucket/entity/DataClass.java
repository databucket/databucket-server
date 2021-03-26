package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import pl.databucket.configuration.Constants;
import pl.databucket.dto.DataClassItemDto;
import pl.databucket.tenant.TenantSupport;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name="data_classes")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Filter(name = "projectFilter", condition = "project_id = :projectId")
public class DataClass extends Auditable<String> implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "class_generator")
    @SequenceGenerator(name="class_generator", sequenceName = "class_seq", allocationSize = 1)
    @Column(name = "class_id", updatable = false, nullable = false)
    private long id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "class_name", length = Constants.NAME_MAX)
    private String name;

    @Column(length = Constants.DESCRIPTION_MAX)
    private String description;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private List<DataClassItemDto> configuration;

    @ManyToMany(mappedBy = "dataClasses")
    private Set<View> views;

    @JsonIgnore
    private Boolean deleted = false;
}
