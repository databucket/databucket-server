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
@Table(name="groups")
@Filter(name = "projectFilter", condition = "project_id = :projectId")
public class Group extends Auditable<String> implements TenantSupport {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_generator")
	@SequenceGenerator(name="group_generator", sequenceName = "group_seq", allocationSize = 1)
	@Column(name = "group_id", updatable = false, nullable = false)
	private long id;

	@Column(name = "project_id", nullable = false)
	private Integer projectId;

	@Column(name = "group_name", length = Constants.NAME_MAX)
	private String name;

	@Column(name = "short_name", length = 5)
	private String shortName;

	@Column(length = Constants.DESCRIPTION_MAX)
	private String description;

	@ManyToMany
	@JoinTable(name = "groups_buckets",
			joinColumns = {	@JoinColumn(name = "group_id") },
			inverseJoinColumns = { @JoinColumn(name = "bucket_id") })
	private Set<Bucket> buckets;

	@ManyToMany
	@JoinTable(name = "groups_teams",
			joinColumns = {@JoinColumn(name = "group_id")},
			inverseJoinColumns = {@JoinColumn(name = "team_id")})
	private Set<Team> teams;

	@ManyToMany
	@JoinTable(name = "groups_users",
			joinColumns = {@JoinColumn(name = "group_id")},
			inverseJoinColumns = {@JoinColumn(name = "user_id")})
	private Set<User> users;

	@OneToOne
	@JoinColumn(name = "role_id", referencedColumnName = "role_id")
	private Role role;

	@JsonIgnore
	private Boolean deleted = false;

	public Set<Long> getBucketsIds() {
		if (buckets != null && buckets.size() > 0)
			return buckets.stream().map(Bucket::getId).collect(Collectors.toSet());
		else
			return null;
	}

	public Set<Long> getUsersIds() {
		if (users != null && users.size() > 0)
			return users.stream().map(User::getId).collect(Collectors.toSet());
		else
			return null;
	}

	public Set<Short> getTeamsIds() {
		if (teams != null && teams.size() > 0)
			return teams.stream().map(Team::getId).collect(Collectors.toSet());
		else
			return null;
	}
}

