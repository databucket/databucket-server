package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.ViewDto;
import pl.databucket.entity.View;

public class ViewPropertyMap extends PropertyMap<View, ViewDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setBucketId(source.getBucket().getId());
        map().setDataClassId(source.getDataClass().getId());
        map().setColumnsId(source.getDataColumns().getId());
        map().setFilterId(source.getDataFilter().getId());
        map().setPrivateItem(source.isPrivateItem());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedDate(source.getCreatedDate());
        map().setLastModifiedBy(source.getLastModifiedBy());
        map().setLastModifiedDate(source.getLastModifiedDate());
    }
}