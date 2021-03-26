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
        map().setFeaturesIds(source.getFeaturesIds());
        map().setBucketsIds(source.getBucketsIds());
        map().setClassesIds(source.getClassesIds());
        map().setColumnsId(source.getDataColumns().getId());
        map().setFilterId(source.getDataFilter().getId());
        map().setRoleId(source.getRole().getId());
        map().setUsersIds(source.getUsersIds());
        map().setTeamsIds(source.getTeamsIds());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedAt(source.getCreatedAt());
        map().setModifiedBy(source.getModifiedBy());
        map().setModifiedAt(source.getModifiedAt());
    }
}