package pl.databucket.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name="roles")
public class Role extends Auditable<String> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_generator")
    @SequenceGenerator(name="role_generator", sequenceName = "role_seq", allocationSize = 1)
    @Column(name = "role_id", updatable = false, nullable = false)
    private long id;

    @Column(name = "role_name", length = 50, unique = true)
    private String name;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users;

}
