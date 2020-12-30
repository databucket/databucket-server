package pl.databucket.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.databucket.model.entity.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByName(String name);
    boolean existsByName(String name);
}
