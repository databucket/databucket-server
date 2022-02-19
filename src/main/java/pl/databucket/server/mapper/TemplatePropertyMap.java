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
//        map().setTemplateConfDto(source.getTemplateConfiguraiton());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedAt(source.getCreatedAt());
        map().setModifiedBy(source.getModifiedBy());
        map().setModifiedAt(source.getModifiedAt());
    }
}