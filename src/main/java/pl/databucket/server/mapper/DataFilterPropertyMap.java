package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.DataFilterDto;
import pl.databucket.server.entity.DataFilter;

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