package pl.databucket.server.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import pl.databucket.server.security.CustomUserDetails;

import java.util.Map;

/**
 * The project of the signed-in user is the Hibernate tenant. Every session reads it when it opens, and
 * Hibernate restricts all entities with a @TenantId field (project_id) to it, on reads and on inserts.
 */
@Component
public class ProjectTenantResolver implements CurrentTenantIdentifierResolver<Integer>, HibernatePropertiesCustomizer {

    // Sessions without a signed-in project (sign-in, startup) match no project data
    private static final Integer NO_PROJECT = -1;

    @Override
    public Integer resolveCurrentTenantIdentifier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof CustomUserDetails userDetails
                && userDetails.getProjectId() != null)
            return userDetails.getProjectId();

        return NO_PROJECT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
