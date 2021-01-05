package pl.databucket.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name="roles")
public class Role extends AuditableAll<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_generator")
    @SequenceGenerator(name="role_generator", sequenceName = "role_seq")
    @Column(name = "role_id", updatable = false, nullable = false)
    private long id;

    @Column(name = "role_name", length = 50, unique = true)
    private String name;

    @Column
    private String description;

}
