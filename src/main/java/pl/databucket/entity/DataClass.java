package pl.databucket.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name="dataclasses")
public class DataClass extends Auditable<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "class_generator")
    @SequenceGenerator(name="class_generator", sequenceName = "class_seq", allocationSize = 1)
    @Column(name = "class_id", updatable = false, nullable = false)
    private long id;

    @Column(name = "class_name", length = 50, unique = true)
    private String name;

    @Column
    private String description;

    @JsonIgnore
    private Boolean deleted = false;
}
