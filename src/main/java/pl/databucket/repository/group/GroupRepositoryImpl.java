package pl.databucket.repository.group;

import org.springframework.stereotype.Repository;
import pl.databucket.database.Condition;
import pl.databucket.model.entity.Group;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class GroupRepositoryImpl implements GroupRepositoryCustom {

    EntityManager entityManager;

    @Override
    public List<Predicate> createPredicates(List<Condition> conditions) {
        List<Predicate> predicates = new ArrayList<>();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery <Group> criteriaQuery = criteriaBuilder.createQuery(Group.class);
        Root<Group> root = criteriaQuery.from(Group.class);

        return predicates;
    }

    @Override
    public Order createOrder(String orderBy) {
        return null;
    }

    @Override
    public List<Group> findByCustomCriteria(List<Predicate> predicates, Order orderBy, Integer offset, Integer limit) {

        CriteriaQuery<Group> criteriaQuery = entityManager.getCriteriaBuilder()
                .createQuery(Group.class)
                .where(predicates.toArray(new Predicate[0]))
                .orderBy(orderBy);

        Query limitedCriteriaQuery = entityManager.createQuery(criteriaQuery)
                .setFirstResult(offset)
                .setMaxResults(limit);

        return limitedCriteriaQuery.getResultList();
    }

    @Override
    public List<Group> findByCustomCriteria(String queryStr) {
        Query query = entityManager.createQuery(queryStr);
        return query.getResultList();
    }

    @Override
    public Integer countByCustomCriteria(List<Predicate> predicates) {
        return null;
    }

    @Override
    public Integer countByCustomCriteria(String queryStr) {
        Query query = entityManager.createQuery(queryStr);
        return query.getMaxResults();
    }
}
