package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.DataColumnsDto;
import pl.databucket.server.entity.DataColumns;

public class DataColumnsPropertyMap extends PropertyMap<DataColumns, DataColumnsDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setClassId(source.getDataClass().getId());
        map().setConfiguration(source.getConfiguration());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedAt(source.getCreatedAt());
        map().setModifiedBy(source.getModifiedBy());
        map().setModifiedAt(source.getModifiedAt());
    }
}