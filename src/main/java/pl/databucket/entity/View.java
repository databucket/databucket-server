package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name="views")
public class View extends Auditable<String> {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "view_generator")
	@SequenceGenerator(name="view_generator", sequenceName = "view_seq", allocationSize = 1)
	@Column(name = "view_id", updatable = false, nullable = false)
	private long id;

	@Column(name = "view_name", length = 50)
	private String name;

	@Column(length = 500)
	private String description;

	@OneToOne
	@JoinColumn(name = "class_id", referencedColumnName = "class_id")
	private DataClass dataClass;

	@OneToOne
	@JoinColumn(name = "bucket_id", referencedColumnName = "bucket_id")
	private Bucket bucket;

	@OneToOne
	@JoinColumn(name = "columns_id", referencedColumnName = "columns_id")
	private Columns columns;

	@OneToOne
	@JoinColumn(name = "filter_id", referencedColumnName = "filter_id")
	private Filter filter;

	@JsonIgnore
	private Boolean deleted = false;
}

