package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.DataClassDto;
import pl.databucket.dto.GroupDto;
import pl.databucket.entity.DataClass;
import pl.databucket.entity.Group;

public class DataClassPropertyMap extends PropertyMap<DataClass, DataClassDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedDate(source.getCreatedDate());
        map().setLastModifiedBy(source.getLastModifiedBy());
        map().setLastModifiedDate(source.getLastModifiedDate());
    }
}