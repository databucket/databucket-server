package pl.databucket.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pl.databucket.server.configuration.Constants;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name="roles")
public class Role extends Auditable<String> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_generator")
    @SequenceGenerator(name="role_generator", sequenceName = "role_seq", allocationSize = 1)
    @Column(name = "role_id", updatable = false, nullable = false)
    private short id;

    @Column(name = "role_name", length = Constants.NAME_MAX, unique = true)
    private String name;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users;

}
