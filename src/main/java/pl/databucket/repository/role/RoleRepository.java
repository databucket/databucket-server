package pl.databucket.repository.role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.model.entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
    boolean existsByName(String name);
}
