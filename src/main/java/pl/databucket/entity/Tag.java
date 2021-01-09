package pl.databucket.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import pl.databucket.tenant.TenantSupport;

import javax.persistence.*;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name="tags")
@Filter(name = "projectFilter", condition = "project_id = :projectId")
public class Tag extends Auditable<String> implements TenantSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tag_generator")
    @SequenceGenerator(name="tag_generator", sequenceName = "tag_seq", allocationSize = 1)
    @Column(name = "tag_id", updatable = false, nullable = false)
	private long id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

	@Column(name = "tag_name", nullable = false)
	private String name;

    private String description = null;

    @ManyToMany
    @JoinTable(name = "tags_buckets",
            joinColumns = {@JoinColumn(name = "tag_id") },
            inverseJoinColumns = {@JoinColumn(name = "bucket_id") })
    private Set<Bucket> buckets;

    @ManyToMany
    @JoinTable(name = "tags_dataclasses",
            joinColumns = {@JoinColumn(name = "tag_id") },
            inverseJoinColumns = {@JoinColumn(name = "class_id") })
    private Set<DataClass> dataClasses;

	private Boolean deleted = false;
}

