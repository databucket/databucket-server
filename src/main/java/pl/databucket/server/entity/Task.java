package pl.databucket.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Filter;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.dto.TaskConfigDto;
import pl.databucket.server.tenant.TenantSupport;

import javax.persistence.*;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor
@Getter
@Setter
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(name="tasks")
@Filter(name = "projectFilter", condition = "project_id = :projectId")
public class Task extends Auditable<String> implements TenantSupport {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "task_generator")
	@SequenceGenerator(name="task_generator", sequenceName = "task_seq", allocationSize = 1)
	@Column(name = "task_id", updatable = false, nullable = false)
	private long id;

	@Column(name = "project_id", nullable = false)
	private Integer projectId;

	@Column(name = "task_name", length = Constants.NAME_MAX)
	private String name;

	@Column(length = Constants.DESCRIPTION_MAX)
	private String description;

	@OneToOne
	@JoinColumn(name = "class_id", referencedColumnName = "class_id")
	private DataClass dataClass; // class support

	@ManyToMany
	@JoinTable(name = "tasks_buckets",
			joinColumns = {@JoinColumn(name = "task_id") },
			inverseJoinColumns = {@JoinColumn(name = "bucket_id")})
	private Set<Bucket> buckets; // visible in buckets

	@ManyToMany
	@JoinTable(name = "tasks_dataclasses",
			joinColumns = {@JoinColumn(name = "task_id")},
			inverseJoinColumns = {@JoinColumn(name = "class_id")})
	private Set<DataClass> dataClasses; // visible by classes

	@OneToOne
	@JoinColumn(name = "filter_id", referencedColumnName = "filter_id")
	private DataFilter dataFilter;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private TaskConfigDto configuration;

	@JsonIgnore
	private Boolean deleted = false;

	public Set<Long> getBucketsIds() {
		if (buckets != null && buckets.size() > 0)
			return buckets.stream().map(Bucket::getId).collect(Collectors.toSet());
		else
			return null;
	}

	public Set<Long> getClassesIds() {
		if (dataClasses != null && dataClasses.size() > 0)
			return dataClasses.stream().map(DataClass::getId).collect(Collectors.toSet());
		else
			return null;
	}
}

