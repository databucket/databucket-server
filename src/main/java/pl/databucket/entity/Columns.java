package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import pl.databucket.dto.ColumnDto;

import javax.persistence.*;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(name="columns")
public class Columns extends AuditableAll<String> {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "columns_generator")
	@SequenceGenerator(name="columns_generator", sequenceName = "columns_seq")
	@Column(name = "columns_id", updatable = false, nullable = false)
	private long id;

	@Column(name = "columns_name", length = 50)
	private String name;

	@Column(length = 500)
	private String description;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "class_id", referencedColumnName = "class_id")
	private DataClass dataClass;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "bucket_id", referencedColumnName = "bucket_id")
	private Bucket bucket;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private List<ColumnDto> columns;

	@JsonIgnore
	private Boolean deleted = false;
}
