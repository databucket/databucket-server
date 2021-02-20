package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import pl.databucket.configuration.Constants;
import pl.databucket.dto.DataFilterItemDto;
import org.hibernate.annotations.Filter;
import pl.databucket.tenant.TenantSupport;

import javax.persistence.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor
@Getter
@Setter
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(name="data_filters")
@Filter(name = "projectFilter", condition = "project_id = :projectId")
public class DataFilter extends Auditable<String> implements TenantSupport {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "filter_generator")
	@SequenceGenerator(name="filter_generator", sequenceName = "filter_seq", allocationSize = 1)
	@Column(name = "filter_id", updatable = false, nullable = false)
	private long id;

	@Column(name = "project_id", nullable = false)
	private Integer projectId;

	@Column(name = "filter_name", length = Constants.NAME_MAX)
	private String name;

	@Column(length = Constants.DESCRIPTION_MAX)
	private String description;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private List<DataFilterItemDto> configuration;

	@JsonIgnore
	private Boolean deleted = false;


}

