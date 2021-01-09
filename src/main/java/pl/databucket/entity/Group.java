package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name="groups")
public class Group extends Auditable<String> {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_generator")
	@SequenceGenerator(name="group_generator", sequenceName = "group_seq", allocationSize = 1)
	@Column(name = "group_id", updatable = false, nullable = false)
	private long id;

	@Column(name = "group_name", length = 50)
	private String name;

	@Column(length = 250)
	private String description;

	@ManyToMany
	@JoinTable(name = "groups_buckets",
			joinColumns = {	@JoinColumn(name = "group_id") },
			inverseJoinColumns = { @JoinColumn(name = "bucket_id") })
	private Set<Bucket> buckets;

	@ManyToMany(mappedBy = "groups")
	private Set<User> users;

	@JsonIgnore
	private Boolean deleted = false;

	public List<Long> getListOfBuckets() {
		return buckets.stream().map(Bucket::getId).collect(Collectors.toList());
	}
}

