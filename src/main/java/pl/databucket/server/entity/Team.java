package pl.databucket.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import pl.databucket.server.configuration.Constants;
import pl.databucket.server.tenant.TenantSupport;

import javax.persistence.*;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name="teams")
@Filter(name = "projectFilter", condition = "project_id = :projectId")
public class Team extends Auditable<String> implements TenantSupport {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "team_generator")
	@SequenceGenerator(name="team_generator", sequenceName = "team_seq", allocationSize = 1)
	@Column(name = "team_id", updatable = false, nullable = false)
	private short id;

	@Column(name = "project_id", nullable = false)
	private Integer projectId;

	@Column(name = "team_name", length = Constants.NAME_MAX)
	private String name;

	@Column(length = Constants.DESCRIPTION_MAX)
	private String description;

	@ManyToMany(mappedBy = "teams")
	private Set<User> users;

	@ManyToMany(mappedBy = "teams")
	private Set<Group> groups;

	@ManyToMany(mappedBy = "teams")
	private Set<Bucket> buckets;

	@ManyToMany(mappedBy = "teams")
	private Set<View> views;

	@JsonIgnore
	private Boolean deleted = false;

	public Set<Long> getUsersIds() {
		if (users != null && users.size() > 0)
			return users.stream().map(User::getId).collect(Collectors.toSet());
		else
			return null;
	}
}

