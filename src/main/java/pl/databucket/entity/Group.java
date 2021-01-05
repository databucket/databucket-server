package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name="groups")
public class Group extends AuditableAll<String> {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "group_generator")
	@SequenceGenerator(name="group_generator", sequenceName = "group_seq")
	@Column(name = "group_id", updatable = false, nullable = false)
	private long id;

	@Column(name = "group_name", length = 50)
	private String name;

	@Column(length = 250)
	private String description;

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "groups_buckets",
			joinColumns = {	@JoinColumn(name = "group_id") },
			inverseJoinColumns = { @JoinColumn(name = "bucket_id") })
	private Set<Bucket> buckets;

	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinTable(name = "groups_users",
			joinColumns = {	@JoinColumn(name = "group_id") },
			inverseJoinColumns = { @JoinColumn(name = "user_id") })
	private Set<User> users;

	@JsonIgnore
	private Boolean deleted = false;
}

