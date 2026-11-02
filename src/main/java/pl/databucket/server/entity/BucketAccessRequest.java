package pl.databucket.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;
import pl.databucket.server.configuration.Constants;

import java.util.Date;

/**
 * A request of a user for access to the data of a bucket, asked on the Overview page and decided by an admin of
 * the project. Approving adds the user to the users of the bucket.
 */
@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "bucket_access_requests")
public class BucketAccessRequest {

    public enum Status {PENDING, APPROVED, REJECTED}

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bucket_access_request_generator")
    @SequenceGenerator(name = "bucket_access_request_generator", sequenceName = "bucket_access_request_seq", allocationSize = 1)
    @Column(name = "request_id", updatable = false, nullable = false)
    private long id;

    @TenantId
    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bucket_id", referencedColumnName = "bucket_id", nullable = false)
    private Bucket bucket;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    @Column(name = "requested_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date requestedAt;

    @Column(name = "decided_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date decidedAt;

    @Column(name = "decided_by", length = Constants.NAME_MAX)
    private String decidedBy;
}
