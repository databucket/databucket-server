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
        map().setEnabledDetails(source.isEnabledDetails());
        map().setEnabledCreation(source.isEnabledCreation());
        map().setEnabledModifying(source.isEnabledModifying());
        map().setEnabledRemoval(source.isEnabledRemoval());
        map().setEnabledImport(source.isEnabledImport());
        map().setEnabledExport(source.isEnabledExport());
        map().setEnabledHistory(source.isEnabledHistory());
        map().setEnabledTasks(source.isEnabledTasks());
        map().setEnabledReservation(source.isEnabledReservation());
        map().setBucketsIds(source.getBucketsIds());
        map().setClassesIds(source.getClassesIds());
        map().setColumnsId(source.getDataColumns().getId());
        map().setFilterId(source.getDataFilter().getId());
        map().setRoleId(source.getRole().getId());
        map().setUsersIds(source.getUsersIds());
        map().setTeamsIds(source.getTeamsIds());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedDate(source.getCreatedDate());
        map().setLastModifiedBy(source.getLastModifiedBy());
        map().setLastModifiedDate(source.getLastModifiedDate());
    }
}