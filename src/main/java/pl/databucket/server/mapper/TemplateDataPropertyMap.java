package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.TemplateDataDto;
import pl.databucket.server.entity.TemplateData;

public class TemplateDataPropertyMap extends PropertyMap<TemplateData, TemplateDataDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setTemplateId(source.getTemplate().getId());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedAt(source.getCreatedAt());
        map().setModifiedBy(source.getModifiedBy());
        map().setModifiedAt(source.getModifiedAt());
    }
}