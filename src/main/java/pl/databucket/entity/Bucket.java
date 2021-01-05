package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name="buckets")
public class Bucket extends AuditableAll<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bucket_generator")
    @SequenceGenerator(name="bucket_generator", sequenceName = "bucket_seq")
    @Column(name = "bucket_id", updatable = false, nullable = false)
    private long id;

    @Column(name = "bucket_name", length = 50)
    private String name;

    @Column(name = "icon_name", length = 50)
    private String iconName;

    @Column
    private String description;

    @OneToOne
    @JoinColumn(name = "class_id", referencedColumnName = "class_id")
    private DataClass dataClass;

    @Column(nullable = false)
    private boolean history = false;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(name = "buckets_users",
            joinColumns = {	@JoinColumn(name = "bucket_id") },
            inverseJoinColumns = { @JoinColumn(name = "user_id") })
    private Set<User> users;

    @JsonIgnore
    private Boolean deleted = false;

}
