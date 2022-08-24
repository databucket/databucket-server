package pl.databucket.server.service.template;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.databucket.server.dto.TemplateDataItemDto;
import pl.databucket.server.entity.TemplateData;
import pl.databucket.server.entity.TemplateDataItem;
import pl.databucket.server.exception.ItemAlreadyExistsException;
import pl.databucket.server.exception.ItemNotFoundException;
import pl.databucket.server.exception.ModifyByNullEntityIdException;
import pl.databucket.server.repository.TemplateDataItemsRepository;
import pl.databucket.server.repository.TemplateDataRepository;

import java.util.List;
import java.util.Optional;


@Service
public class TemplateDataItemsService {

    @Autowired
    private TemplateDataRepository templateDataRepository;

    @Autowired
    private TemplateDataItemsRepository templateDataItemsRepository;

    public TemplateDataItem createTemplateDataItem(TemplateDataItemDto templateDataItemDto) throws ItemAlreadyExistsException, ItemNotFoundException {
        Optional<TemplateData> templateDataOpt = templateDataRepository.findById(templateDataItemDto.getDataId());

        if (!templateDataOpt.isPresent())
            throw new ItemNotFoundException(TemplateData.class, templateDataItemDto.getDataId());

        TemplateDataItem templateDataItem = new TemplateDataItem();
        templateDataItem.setTemplateData(templateDataOpt.get());
        templateDataItem.setTagUid(templateDataItemDto.getTagUid());
        if (templateDataItemDto.getReserved() != null ) {
            if (templateDataItemDto.getReserved()) {
                templateDataItem.setReserved(true);
                if (templateDataItem.getOwner() != null)
                    templateDataItem.setOwner(templateDataItemDto.getOwner());
                else
                    templateDataItem.setOwner("user");
            } else
                templateDataItem.setReserved(false);
        }
        if (templateDataItemDto.getProperties() != null)
            templateDataItem.setProperties(templateDataItemDto.getProperties());

        return templateDataItemsRepository.save(templateDataItem);
    }

    public List<TemplateDataItem> getTemplateDataItems(int templateDataId) throws ItemNotFoundException {
        Optional<TemplateData> templateDataOpt = templateDataRepository.findById(templateDataId);

        if (!templateDataOpt.isPresent())
            throw new ItemNotFoundException(TemplateData.class, templateDataId);

        return templateDataItemsRepository.findAllByTemplateData(templateDataOpt.get());
    }

    public TemplateDataItem modifyTemplateDataItem(TemplateDataItemDto templateDataItemDto) throws ItemNotFoundException, ItemAlreadyExistsException, ModifyByNullEntityIdException {
        Optional<TemplateData> templateDataOpt = templateDataRepository.findById(templateDataItemDto.getDataId());
        if (!templateDataOpt.isPresent())
            throw new ItemNotFoundException(TemplateData.class, templateDataItemDto.getDataId());

        Optional<TemplateDataItem> templateDataItemOpt = templateDataItemsRepository.findById(templateDataItemDto.getId());
        if (!templateDataItemOpt.isPresent())
            throw new ItemNotFoundException(TemplateDataItem.class, templateDataItemDto.getId());

        TemplateDataItem templateDataItem = templateDataItemOpt.get();
        templateDataItem.setTemplateData(templateDataOpt.get());
        templateDataItem.setTagUid(templateDataItemDto.getTagUid());
        if (templateDataItemDto.getReserved() != null ) {
            if (templateDataItemDto.getReserved()) {
                templateDataItem.setReserved(true);
                if (templateDataItem.getOwner() != null)
                    templateDataItem.setOwner(templateDataItemDto.getOwner());
                else
                    templateDataItem.setOwner("user");
            } else {
                templateDataItem.setReserved(false);
                templateDataItem.setOwner(null);
            }
        }
        if (templateDataItemDto.getProperties() != null)
            templateDataItem.setProperties(templateDataItemDto.getProperties());
        else
            templateDataItem.setProperties(null);
        return templateDataItemsRepository.save(templateDataItem);
    }

    public void deleteTemplateDataItem(long templateDataItemId) throws ItemNotFoundException {
        Optional<TemplateDataItem> templateDataItemOpt = templateDataItemsRepository.findById(templateDataItemId);

        if (!templateDataItemOpt.isPresent())
            throw new ItemNotFoundException(TemplateDataItem.class, templateDataItemId);

        templateDataItemsRepository.delete(templateDataItemOpt.get());
    }

    public void deleteTemplateDataItems(TemplateData templateData) throws ItemNotFoundException {
        templateDataItemsRepository.deleteAllByTemplateData(templateData);
    }
}
