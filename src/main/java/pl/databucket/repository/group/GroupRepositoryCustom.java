package pl.databucket.repository.group;

import pl.databucket.database.Condition;
import pl.databucket.model.Group;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import java.util.List;

public interface GroupRepositoryCustom {

    List<Predicate> createPredicates(List<Condition> conditions);
    Order createOrder(String orderBy);
    List<Group> findByCustomCriteria(String query);
    List<Group> findByCustomCriteria(List<Predicate> predicates, Order orderBy, Integer offset, Integer limit);
    Integer countByCustomCriteria(String query);
    Integer countByCustomCriteria(List<Predicate> predicates);

}
