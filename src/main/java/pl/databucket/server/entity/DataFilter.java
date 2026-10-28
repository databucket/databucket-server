package pl.databucket.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.DataFilterConfigDto;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name="data_filters")
public class DataFilter extends Auditable<String> {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "filter_generator")
	@SequenceGenerator(name="filter_generator", sequenceName = "filter_seq", allocationSize = 1)
	@Column(name = "filter_id", updatable = false, nullable = false)
	private long id;

	@TenantId
	@Column(name = "project_id", nullable = false)
	private Integer projectId;

	@Column(name = "filter_name", length = Constants.NAME_MAX)
	private String name;

	@Column(length = Constants.DESCRIPTION_MAX)
	private String description;

	@ManyToOne
	@JoinColumn(name = "class_id", referencedColumnName = "class_id")
	private DataClass dataClass;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private DataFilterConfigDto configuration;

	@JsonIgnore
	private Boolean deleted = false;


}

