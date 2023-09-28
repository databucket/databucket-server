package pl.databucket.server.tenant;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import pl.databucket.server.security.TokenProvider;

public class EntityInterceptor extends EmptyInterceptor {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        if (entity instanceof TenantSupport) {
            setProjectId(state, propertyNames);
        }
        return super.onSave(entity, id, state, propertyNames, types);
    }

    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState,
        String[] propertyNames, Type[] types) {
        if (entity instanceof TenantSupport) {
            setProjectId(currentState, propertyNames);
        }
        return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
    }

    private void setProjectId(Object[] currentState, String[] propertyNames) {
        Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long projectId = jwt.getClaim(TokenProvider.PROJECT_ID);
        currentState[List.of(propertyNames).indexOf("projectId")] = projectId.intValue();
    }
}
