package pl.databucket.server.tenant;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.security.core.context.SecurityContextHolder;
import pl.databucket.server.security.CustomUserDetails;

import java.io.Serializable;

public class EntityInterceptor extends EmptyInterceptor {

    private static final long serialVersionUID = 1L;

    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        if (entity instanceof TenantSupport) {
            setProjectId(state, propertyNames);
        }
        return super.onSave(entity, id, state, propertyNames, types);
    }

    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        if (entity instanceof TenantSupport) {
            setProjectId(currentState, propertyNames);
        }
        return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
    }

    private void setProjectId(Object[] currentState, String[] propertyNames) {
        currentState[ArrayUtils.indexOf(propertyNames, "projectId")] = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getProjectId();
    }
}