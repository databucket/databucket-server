package pl.databucket.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.server.entity.Template;

import java.util.List;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Integer> {
    boolean existsTemplateByName(String name);
    List<Template> findAllByIdIn(Iterable<Integer> ids);
}
