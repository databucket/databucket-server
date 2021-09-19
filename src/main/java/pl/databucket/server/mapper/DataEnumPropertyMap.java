package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.DataEnumDto;
import pl.databucket.server.entity.DataEnum;

public class DataEnumPropertyMap extends PropertyMap<DataEnum, DataEnumDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setIconsEnabled(source.isIconsEnabled());
        map().setItems(source.getItems());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedAt(source.getCreatedAt());
        map().setModifiedBy(source.getModifiedBy());
        map().setModifiedAt(source.getModifiedAt());
    }
}