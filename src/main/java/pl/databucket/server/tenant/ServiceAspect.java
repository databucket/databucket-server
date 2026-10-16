package pl.databucket.server.tenant;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pl.databucket.server.security.CustomUserDetails;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Aspect
@Component
// Runs inside the transaction (ordered 0 in ApplicationConfiguration), so the filter lands on its session.
@Order(1)
public class ServiceAspect {

    @PersistenceContext
    EntityManager em;

    // @formatter:off
    @Before("execution(* pl.databucket.server.service.BucketService.*(..))"
            + " || execution(* pl.databucket.server.service.GroupService.*(..)) "
            + " || execution(* pl.databucket.server.service.DataClassService.*(..)) "
            + " || execution(* pl.databucket.server.service.DataColumnsService.*(..)) "
            + " || execution(* pl.databucket.server.service.DataFilterService.*(..)) "
            + " || execution(* pl.databucket.server.service.TagService.*(..)) "
            + " || execution(* pl.databucket.server.service.TaskService.*(..)) "
            + " || execution(* pl.databucket.server.service.ViewService.*(..)) "
            + " || execution(* pl.databucket.server.service.DataEnumService.*(..)) "
            + " || execution(* pl.databucket.server.service.TeamService.*(..)) "
            + " || execution(* pl.databucket.server.service.SvgService.*(..)) "
    )
    // @formatter:on
    public void aroundExecution(JoinPoint pjp) throws Throwable {
        // Outside a transaction the shared EntityManager hands out a throwaway session, so the filter
        // would be enabled on nothing and queries would return data of every project. Fail instead.
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Tenant filtered service called outside a transaction: " + pjp.getSignature().toShortString());

        Filter filter = em.unwrap(Session.class).enableFilter("projectFilter");
        filter.setParameter("projectId", ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getProjectId());
        filter.validate();
    }
}
