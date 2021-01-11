package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import pl.databucket.configuration.Constants;
import pl.databucket.dto.ColumnDto;
import org.hibernate.annotations.Filter;
import pl.databucket.tenant.TenantSupport;

import javax.persistence.*;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(name="data_columns")
@Filter(name = "projectFilter", condition = "project_id = :projectId")
public class DataColumns extends Auditable<String> implements TenantSupport {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "columns_generator")
	@SequenceGenerator(name="columns_generator", sequenceName = "columns_seq", allocationSize = 1)
	@Column(name = "columns_id", updatable = false, nullable = false)
	private long id;

	@Column(name = "project_id", nullable = false)
	private Integer projectId;

	@Column(name = "columns_name", length = Constants.NAME_MAX)
	private String name;

	@Column(length = Constants.DESCRIPTION_MAX)
	private String description;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "class_id", referencedColumnName = "class_id")
	private DataClass dataClass;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "bucket_id", referencedColumnName = "bucket_id")
	private Bucket bucket;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private List<ColumnDto> columns;

	@JsonIgnore
	private Boolean deleted = false;
}

