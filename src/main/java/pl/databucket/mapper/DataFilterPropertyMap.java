package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.DataFilterDto;
import pl.databucket.entity.DataFilter;

public class DataFilterPropertyMap extends PropertyMap<DataFilter, DataFilterDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setConfiguration(source.getConfiguration());
        map().setClassId(source.getDataClass().getId());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedAt(source.getCreatedAt());
        map().setModifiedBy(source.getModifiedBy());
        map().setModifiedAt(source.getModifiedAt());
    }
}