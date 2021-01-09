package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.Filter;
import pl.databucket.tenant.TenantSupport;

import javax.persistence.*;
import java.util.Map;
import java.util.Set;

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

	@Column(name = "task_name", length = 50)
	private String name;

	@Column(length = 500)
	private String description;

	@ManyToMany
	@JoinTable(name = "tasks_buckets",
			joinColumns = {@JoinColumn(name = "task_id") },
			inverseJoinColumns = {@JoinColumn(name = "bucket_id")})
	private Set<Bucket> buckets;

	@ManyToMany
	@JoinTable(name = "tasks_dataclasses",
			joinColumns = {@JoinColumn(name = "task_id")},
			inverseJoinColumns = {@JoinColumn(name = "class_id")})
	private Set<DataClass> dataClasses;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private Map<String, Object> configuration;

	@JsonIgnore
	private Boolean deleted = false;
}

