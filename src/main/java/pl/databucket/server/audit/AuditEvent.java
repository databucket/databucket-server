package pl.databucket.server.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * A change of a project's settings or of the management area (Settings > Activity, Management > Audit log). Written by
 * {@link AuditListener} with plain SQL in the transaction of the change; this entity only defines the table. Not
 * restricted to a project (no @TenantId): the management area reads all of it, Settings filters by project_id.
 */
@Getter
@Setter
@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "audit_events_project", columnList = "project_id, event_id"),
        @Index(name = "audit_events_at", columnList = "event_at")})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id", updatable = false, nullable = false)
    private long id;

    @Column(name = "event_at", nullable = false)
    private Instant at;

    // null: an object outside the projects (users) or the change of a project itself is under its own id
    @Column(name = "project_id")
    private Integer projectId;

    @Column(nullable = false, length = 50)
    private String actor;

    @Column(name = "actor_role", length = 10)
    private String actorRole;

    // WEB, API, MCP, SCHEDULER, SYSTEM
    @Column(nullable = false, length = 10)
    private String source;

    // CREATE, UPDATE, DELETE, or an operation of the operation log (e.g. auth.signin, project.clear): then changes
    // holds its details (key -> value) instead of [before, after]
    @Column(nullable = false, length = 40)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 30)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 40)
    private String entityId;

    @Column(name = "entity_name", length = 100)
    private String entityName;

    // property -> [before, after]
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> changes;

    @Column(name = "request_id", length = 50)
    private String requestId;
}
