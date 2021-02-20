package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.DataEnumDto;
import pl.databucket.entity.DataEnum;

public class DataEnumPropertyMap extends PropertyMap<DataEnum, DataEnumDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setTextValues(source.isTextValues());
        map().setItems(source.getItems());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedDate(source.getCreatedDate());
        map().setLastModifiedBy(source.getLastModifiedBy());
        map().setLastModifiedDate(source.getLastModifiedDate());
    }
}