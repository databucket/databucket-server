package pl.databucket.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.databucket.server.entity.TemplateData;
import pl.databucket.server.entity.TemplateDataItem;

import java.util.List;

@Repository
public interface TemplateDataItemsRepository extends JpaRepository<TemplateDataItem, Long> {
    List<TemplateDataItem> findAllByTemplateData(TemplateData templateData);
    void deleteAllByTemplateData(TemplateData templateData);
}
