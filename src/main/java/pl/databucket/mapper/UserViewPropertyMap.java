package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.UserViewDto;
import pl.databucket.entity.View;

public class UserViewPropertyMap extends PropertyMap<View, UserViewDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setFeaturesIds(source.getFeaturesIds());
        map().setBucketsIds(source.getBucketsIds());
        map().setClassesIds(source.getClassesIds());
        map().setColumnsId(source.getDataColumns().getId());
        map().setFilterId(source.getDataFilter().getId());
    }
}