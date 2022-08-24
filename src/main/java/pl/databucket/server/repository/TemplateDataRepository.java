package pl.databucket.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.server.entity.Template;
import pl.databucket.server.entity.TemplateData;

import java.util.List;

@Repository
public interface TemplateDataRepository extends JpaRepository<TemplateData, Integer> {

    List<TemplateData> findAllByTemplate(Template template);

}
