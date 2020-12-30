package pl.databucket.repository.group;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.databucket.model.entity.Group;

public interface GroupRepository extends JpaRepository<Group, Integer>, GroupRepositoryCustom {

    boolean existsByName(String name);
}
