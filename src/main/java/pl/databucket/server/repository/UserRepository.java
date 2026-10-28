package pl.databucket.server.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.server.entity.Project;
import pl.databucket.server.entity.User;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String name);
    @EntityGraph(attributePaths = {"projects", "roles"})
    User findWithProjectsAndRolesByUsername(String name);
    User findByEmailIgnoreCase(String email);
    boolean existsByUsername(String name);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, long id);
    List<User> findAllByIdIn(Iterable<Long> ids);
    List<User> findAllByOrderById();
    List<User> findUsersByProjectsContainsOrderById(Project project);
    List<User> findAllByUsernameIn(Iterable<String> userNames);
}
