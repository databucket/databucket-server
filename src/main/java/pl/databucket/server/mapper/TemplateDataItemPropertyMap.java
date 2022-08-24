package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.TemplateDataItemDto;
import pl.databucket.server.entity.TemplateDataItem;

public class TemplateDataItemPropertyMap extends PropertyMap<TemplateDataItem, TemplateDataItemDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setDataId(source.getTemplateData().getId());
        map().setTagUid(source.getTagUid());
        map().setOwner(source.getOwner());
        map().setReserved(source.isReserved());
        map().setProperties(source.getProperties());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedAt(source.getCreatedAt());
        map().setModifiedBy(source.getModifiedBy());
        map().setModifiedAt(source.getModifiedAt());
    }
}