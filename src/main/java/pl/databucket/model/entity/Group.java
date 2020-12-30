package pl.databucket.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;

import javax.persistence.*;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Table(name="groups")
public class Group extends Auditable<String> {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "group_id")
	private Integer id;

	@Column(name = "group_name", length = 50)
	private String name;

	@Column(length = 500)
	private String description;

	@Type(type = "jsonb")
	@Column(columnDefinition = "jsonb")
	private List<Integer> buckets;

	@JsonIgnore
	private Boolean deleted = false;
}

