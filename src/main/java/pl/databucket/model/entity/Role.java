package pl.databucket.model.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name="roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_generator")
    @SequenceGenerator(name="role_generator", sequenceName = "role_seq")
    @Column(updatable = false, nullable = false)
    private long id;

    @Column(length = 50, unique = true)
    private String name;

    @Column
    private String description;

}
