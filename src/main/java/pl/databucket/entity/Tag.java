package pl.databucket.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name="tags")
public class Tag extends AuditableAll<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tag_generator")
    @SequenceGenerator(name="tag_generator", sequenceName = "tag_seq")
    @Column(name = "tag_id", updatable = false, nullable = false)
	private long id;

	@Column(name = "tag_name", nullable = false)
	private String name;

    private String description = null;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "tags_buckets",
            joinColumns = {@JoinColumn(name = "tag_id") },
            inverseJoinColumns = {@JoinColumn(name = "bucket_id") })
    private Set<Bucket> buckets;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "tags_dataclasses",
            joinColumns = {@JoinColumn(name = "tag_id") },
            inverseJoinColumns = {@JoinColumn(name = "class_id") })
    private Set<DataClass> dataClasses;

	private Boolean deleted = false;
}

