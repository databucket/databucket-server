package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import pl.databucket.dto.CriteriaDto;
import org.hibernate.annotations.Filter;
import pl.databucket.tenant.TenantSupport;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

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

	@Column(name = "filter_name", length = 50)
	private String name;

	@Column(length = 500)
	private String description;

	@ManyToMany
	@JoinTable(name = "filter_buckets",
			joinColumns = {@JoinColumn(name = "filter_id") },
			inverseJoinColumns = {@JoinColumn(name = "bucket_id")})
	private Set<Bucket> buckets;

	@ManyToMany
	@JoinTable(name = "filter_dataclasses",
			joinColumns = {@JoinColumn(name = "filter_id")},
			inverseJoinColumns = {@JoinColumn(name = "class_id")})
	private Set<DataClass> dataClasses;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private List<CriteriaDto> criteria;

	@JsonIgnore
	private Boolean deleted = false;
}

