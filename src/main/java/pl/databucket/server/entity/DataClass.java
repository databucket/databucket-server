package pl.databucket.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.DataClassItemDto;

import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name="data_classes")
public class DataClass extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "class_generator")
    @SequenceGenerator(name="class_generator", sequenceName = "class_seq", allocationSize = 1)
    @Column(name = "class_id", updatable = false, nullable = false)
    private long id;

    @TenantId
    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "class_name", length = Constants.NAME_MAX)
    private String name;

    @Column(length = Constants.DESCRIPTION_MAX)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<DataClassItemDto> configuration;

    @ManyToMany(mappedBy = "dataClasses")
    private Set<View> views;

    @ManyToMany(mappedBy = "dataClasses")
    private Set<Tag> tags;

    @JsonIgnore
    private Boolean deleted = false;
}
