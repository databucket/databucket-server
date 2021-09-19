package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.AccessTreeViewDto;
import pl.databucket.server.entity.View;

public class AccessTreeViewPropertyMap extends PropertyMap<View, AccessTreeViewDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setBucketsIds(source.getBucketsIds());
        map().setClassesIds(source.getClassesIds());
        map().setColumnsId(source.getDataColumns().getId());
        map().setFilterId(source.getDataFilter().getId());
        map().setFeaturesIds(source.getFeaturesIds());
    }
}