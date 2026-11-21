package pl.databucket.server.audit;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import pl.databucket.server.logging.AuditLog;

import java.sql.Timestamp;
import java.time.Instant;

/**
 * Records some operations of the operation log in the audit table (see {@link AuditActions}): the operation log calls
 * it after the commit of the operation, and the event is written in a transaction of its own.
 */
@Component
@ConditionalOnProperty(name = "databucket.audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditActionRecorder {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public AuditActionRecorder(NamedParameterJdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        // the operation's transaction is over (after commit): never take part in it
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @PostConstruct
    public void register() {
        AuditLog.setSink(this::record);
    }

    @PreDestroy
    public void unregister() {
        AuditLog.setSink(null);
    }

    void record(String action, String message, Object[] args) {
        AuditActions.Action recorded = AuditActions.of(action, message, args, AuditContext.source());
        if (recorded == null)
            return;
        Integer projectId = recorded.projectId() != null ? recorded.projectId() : AuditContext.project();
        // a sign-in (also a failed one) has no signed-in user yet: the user it names
        String actor = AuditContext.actor();
        if ("system".equals(actor) && recorded.details().get("user") != null)
            actor = String.valueOf(recorded.details().get("user"));
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("at", Timestamp.from(Instant.now()))
                .addValue("project", projectId)
                .addValue("actor", AuditContext.truncate(actor, 50))
                .addValue("role", AuditContext.role())
                .addValue("source", AuditContext.source())
                .addValue("action", AuditContext.truncate(action, 40))
                .addValue("type", recorded.entityType())
                .addValue("name", AuditContext.truncate(recorded.entityName(), 100))
                .addValue("changes", recorded.details().isEmpty() ? null : AuditChanges.toJson(recorded.details()))
                .addValue("request", AuditContext.truncate(AuditContext.requestId(), 50));
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.update("INSERT INTO audit_events (event_at,"
                + " project_id, actor, actor_role, source, action, entity_type, entity_id, entity_name, changes, request_id)"
                + " VALUES (:at, :project, :actor, :role, :source, :action, :type, '', :name, CAST(:changes AS jsonb), :request)",
                params));
    }
}
