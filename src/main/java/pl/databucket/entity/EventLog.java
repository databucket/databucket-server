package pl.databucket.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name="event_log")
public class EventLog extends AuditableCreatedDate<String> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_log_generator")
    @SequenceGenerator(name="event_log_generator", sequenceName = "event_log_seq")
    @Column(name = "event_log_id", updatable = false, nullable = false)
	private long id;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "event_id", referencedColumnName = "event_id", nullable = false)
    private Event event;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "task_id", referencedColumnName = "task_id", nullable = false)
    private Task task;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "bucket_id", referencedColumnName = "bucket_id", nullable = false)
    private Bucket bucket;

    @Column(nullable = false)
    private Integer affected;

	private Boolean deleted = false;
}

