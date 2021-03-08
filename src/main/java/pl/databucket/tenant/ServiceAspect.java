package pl.databucket.tenant;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import pl.databucket.security.CustomUserDetails;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Aspect
@Component
public class ServiceAspect {

    @PersistenceContext
    EntityManager em;

    // @formatter:off
    @Before("execution(* pl.databucket.service.BucketService.*(..))"
            + " || execution(* pl.databucket.service.GroupService.*(..)) "
            + " || execution(* pl.databucket.service.DataClassService.*(..)) "
            + " || execution(* pl.databucket.service.DataColumnsService.*(..)) "
            + " || execution(* pl.databucket.service.DataFilterService.*(..)) "
            + " || execution(* pl.databucket.service.EventLogService.*(..)) "
            + " || execution(* pl.databucket.service.EventService.*(..)) "
            + " || execution(* pl.databucket.service.TagService.*(..)) "
            + " || execution(* pl.databucket.service.TaskService.*(..)) "
            + " || execution(* pl.databucket.service.ViewService.*(..)) "
            + " || execution(* pl.databucket.service.DataEnumService.*(..)) "
            + " || execution(* pl.databucket.service.TeamService.*(..)) "
    )
    // @formatter:on
    public void aroundExecution(JoinPoint pjp) throws Throwable {
        Filter filter = em.unwrap(Session.class).enableFilter("projectFilter");
        filter.setParameter("projectId", ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getProjectId());
        filter.validate();
    }
}
