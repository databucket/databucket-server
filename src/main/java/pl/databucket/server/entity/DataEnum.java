package pl.databucket.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.DataEnumItemDto;
import pl.databucket.server.tenant.TenantSupport;

import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name="data_enum")
@Filter(name = "projectFilter", condition = "project_id = :projectId")
public class DataEnum extends Auditable<String> implements TenantSupport {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "columns_generator")
	@SequenceGenerator(name="columns_generator", sequenceName = "columns_seq", allocationSize = 1)
	@Column(name = "enum_id", updatable = false, nullable = false)
	private int id;

	@Column(name = "project_id", nullable = false)
	private Integer projectId;

	@Column(name = "enum_name", length = Constants.NAME_MAX)
	private String name;

	@Column(name = "icons_enabled")
	private boolean iconsEnabled;

	@Column(length = Constants.DESCRIPTION_MAX)
	private String description;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private List<DataEnumItemDto> items;

	@JsonIgnore
	private Boolean deleted = false;
}

