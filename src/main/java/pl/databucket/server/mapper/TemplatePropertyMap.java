package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.TemplateDto;
import pl.databucket.server.entity.Template;

public class TemplatePropertyMap extends PropertyMap<Template, TemplateDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setTemplatesIds(source.getTemplatesIds());
        map().setProjectsIds(source.getProjectsIds());
        map().setConfiguration(source.getConfiguration());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedAt(source.getCreatedAt());
        map().setModifiedBy(source.getModifiedBy());
        map().setModifiedAt(source.getModifiedAt());
    }
}