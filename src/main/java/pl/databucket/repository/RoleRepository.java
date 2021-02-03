package pl.databucket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Role;

import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, Short> {
    Role findByName(String name);
    boolean existsByName(String name);
    List<Role> findAllByOrderByIdAsc();
}
