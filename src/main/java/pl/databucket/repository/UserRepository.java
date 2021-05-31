package pl.databucket.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.entity.Project;
import pl.databucket.entity.User;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String name);
    boolean existsByUsername(String name);
    List<User> findAllByIdIn(Iterable<Long> ids);
    List<User> findAllByOrderById();
    List<User> findUsersByProjectsContainsOrderById(Project project);
}
