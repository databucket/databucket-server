package pl.databucket.server.tenant;

import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pl.databucket.server.security.CustomUserDetails;
import java.util.Arrays;

public class EntityInterceptor implements Interceptor {

    @Override
    public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        if (entity instanceof TenantSupport) {
            return setProjectId(state, propertyNames);
        }
        return false;
    }

    @Override
    public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState,
                                String[] propertyNames, Type[] types) {
        if (entity instanceof TenantSupport) {
            return setProjectId(currentState, propertyNames);
        }
        return false;
    }

    private boolean setProjectId(Object[] currentState, String[] propertyNames) {
        int index = Arrays.asList(propertyNames).indexOf("projectId");
        if (index >= 0) {
            Integer projectId = extractProjectIdFromSecurityContext();
            if (projectId != null && !projectId.equals(currentState[index])) {
                currentState[index] = projectId;
                return true;
            }
        }
        return false;
    }

    private Integer extractProjectIdFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getProjectId();
        }

        return null;
    }
}
