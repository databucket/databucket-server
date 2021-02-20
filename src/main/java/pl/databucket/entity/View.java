package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import pl.databucket.configuration.Constants;
import pl.databucket.tenant.TenantSupport;

import javax.persistence.*;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name="views")
@Filter(name = "projectFilter", condition = "project_id = :projectId")
public class View extends Auditable<String> implements TenantSupport {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "view_generator")
	@SequenceGenerator(name="view_generator", sequenceName = "view_seq", allocationSize = 1)
	@Column(name = "view_id", updatable = false, nullable = false)
	private long id;

	@Column(name = "project_id", nullable = false)
	private Integer projectId;

	@Column(name = "view_name", length = Constants.NAME_MAX)
	private String name;

	@Column(length = Constants.DESCRIPTION_MAX)
	private String description;

	@Column(name = "private", nullable = false)
	private boolean privateItem = false;

	@OneToOne
	@JoinColumn(name = "class_id", referencedColumnName = "class_id")
	private DataClass dataClass;

	@OneToOne
	@JoinColumn(name = "bucket_id", referencedColumnName = "bucket_id")
	private Bucket bucket;

	@OneToOne
	@JoinColumn(name = "columns_id", referencedColumnName = "columns_id")
	private DataColumns dataColumns;

	@OneToOne
	@JoinColumn(name = "filter_id", referencedColumnName = "filter_id")
	private DataFilter dataFilter;

	@ManyToMany(mappedBy = "groups")
	private Set<User> users;

	@JsonIgnore
	private Boolean deleted = false;

	public Set<Long> getUsersIds() {
		if (users != null && users.size() > 0)
			return users.stream().map(User::getId).collect(Collectors.toSet());
		else
			return null;
	}
}

