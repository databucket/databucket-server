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

	@Column(name = "features")
	private Short[] featuresIds; // hibernate has problem with Set<Short>

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "views_classes",
			joinColumns = {@JoinColumn(name = "view_id")},
			inverseJoinColumns = {@JoinColumn(name = "class_id")})
	private Set<DataClass> dataClasses;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "views_buckets",
			joinColumns = {@JoinColumn(name = "view_id")},
			inverseJoinColumns = {@JoinColumn(name = "bucket_id")})
	private Set<Bucket> buckets;

	@OneToOne
	@JoinColumn(name = "columns_id", referencedColumnName = "columns_id")
	private DataColumns dataColumns;

	@OneToOne
	@JoinColumn(name = "filter_id", referencedColumnName = "filter_id")
	private DataFilter dataFilter;


	@ManyToMany
	@JoinTable(name = "views_teams",
			joinColumns = {@JoinColumn(name = "view_id")},
			inverseJoinColumns = {@JoinColumn(name = "team_id")})
	private Set<Team> teams;

	@ManyToMany
	@JoinTable(name = "views_users",
			joinColumns = {@JoinColumn(name = "view_id")},
			inverseJoinColumns = {@JoinColumn(name = "user_id")})
	private Set<User> users;

	@OneToOne
	@JoinColumn(name = "role_id", referencedColumnName = "role_id")
	private Role role;

	@JsonIgnore
	private Boolean deleted = false;

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

