package pl.databucket.server.service.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.TemplateDataDto;
import pl.databucket.server.entity.Template;
import pl.databucket.server.entity.TemplateData;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.TemplateDataRepository;
import pl.databucket.server.repository.TemplateRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TemplateDataService {

    @Autowired
    private TemplateDataRepository templateDataRepository;

    @Autowired
    private TemplateRepository templateRepository;

    public TemplateData createTemplateData(TemplateDataDto templateDataDto) throws ItemAlreadyExistsException, ItemNotFoundException {
        Optional<Template> templateOpt = templateRepository.findById(templateDataDto.getTemplateId());

        if (!templateOpt.isPresent())
            throw new ItemNotFoundException(Template.class, templateDataDto.getTemplateId());

        TemplateData templateData = new TemplateData();
        templateData.setName(templateDataDto.getName());
        templateData.setDescription(templateDataDto.getDescription());
        templateData.setTemplate(templateOpt.get());

        return templateDataRepository.save(templateData);
    }

    public List<TemplateData> getTemplateData(int templateId) throws ItemNotFoundException {
        Optional<Template> templateOpt = templateRepository.findById(templateId);

        if (!templateOpt.isPresent())
            throw new ItemNotFoundException(Template.class, templateId);

        return templateDataRepository.findAllByTemplate(templateOpt.get());
    }

    public TemplateData modifyTemplateData(TemplateDataDto templateDataDto) throws ItemNotFoundException, ItemAlreadyExistsException, ModifyByNullEntityIdException {
        if (templateDataDto.getId() == null)
            throw new ModifyByNullEntityIdException(TemplateData.class);

        Optional<TemplateData> templateDataOpt = templateDataRepository.findById(templateDataDto.getId());

        if (!templateDataOpt.isPresent())
            throw new ItemNotFoundException(TemplateData.class, templateDataDto.getId());

        TemplateData templateData = templateDataOpt.get();
        templateData.setName(templateDataDto.getName());
        templateData.setDescription(templateDataDto.getDescription());

        return templateDataRepository.save(templateData);
    }

    public void deleteTemplateData(TemplateData templateData) throws ItemNotFoundException {
        templateDataRepository.delete(templateData);
    }
}
