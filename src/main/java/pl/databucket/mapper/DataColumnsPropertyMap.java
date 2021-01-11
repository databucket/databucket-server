package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.DataColumnsDto;
import pl.databucket.entity.DataColumns;

public class DataColumnsPropertyMap extends PropertyMap<DataColumns, DataColumnsDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setBucketId(source.getBucket().getId());
        map().setDataClassId(source.getDataClass().getId());
        map().setColumns(source.getColumns());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedDate(source.getCreatedDate());
        map().setLastModifiedBy(source.getLastModifiedBy());
        map().setLastModifiedDate(source.getLastModifiedDate());
    }
}