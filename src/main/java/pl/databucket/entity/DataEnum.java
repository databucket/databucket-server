package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import pl.databucket.configuration.Constants;
import pl.databucket.dto.DataEnumItemDto;
import pl.databucket.tenant.TenantSupport;

import javax.persistence.*;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
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

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private List<DataEnumItemDto> items;

	@JsonIgnore
	private Boolean deleted = false;
}

