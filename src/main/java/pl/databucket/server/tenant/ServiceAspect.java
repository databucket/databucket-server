package pl.databucket.server.tenant;

import java.util.Objects;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import pl.databucket.server.security.TokenProvider;

@Aspect
@Component
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
        Filter filter = em.unwrap(Session.class).enableFilter("projectFilter");
        Object projectId = ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
            .getClaim(TokenProvider.PROJECT_ID);
        filter.setParameter("projectId", Optional.ofNullable(projectId)
            .map(Objects::toString)
            .map(Integer::parseInt
            ).orElse(null));
        filter.validate();
    }
}
