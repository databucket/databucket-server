package pl.databucket.server.audit;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Adds {@link AuditListener} to Hibernate (databucket.audit.enabled; off in the tests on H2, which has no jsonb). */
@Component
@ConditionalOnProperty(name = "databucket.audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditListenerRegistration {

    private final EntityManagerFactory entityManagerFactory;

    public AuditListenerRegistration(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @PostConstruct
    public void register() {
        EventListenerRegistry registry = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry().getService(EventListenerRegistry.class);
        AuditListener listener = new AuditListener();
        registry.appendListeners(EventType.POST_INSERT, listener);
        registry.appendListeners(EventType.POST_UPDATE, listener);
        registry.appendListeners(EventType.POST_DELETE, listener);
        registry.appendListeners(EventType.POST_COLLECTION_RECREATE, listener);
        registry.appendListeners(EventType.POST_COLLECTION_UPDATE, listener);
        registry.appendListeners(EventType.POST_COLLECTION_REMOVE, listener);
        registry.appendListeners(EventType.PRE_COLLECTION_REMOVE, listener);
    }
}
