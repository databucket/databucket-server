package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.ManageProjectDto;
import pl.databucket.entity.Project;

public class ManageProjectPropertyMap extends PropertyMap<Project, ManageProjectDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setEnabled(source.getEnabled());
        map().setExpirationDate(source.getExpirationDate());
        map().setUsersIds(source.getUsersIds());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedAt(source.getCreatedAt());
        map().setModifiedBy(source.getModifiedBy());
        map().setModifiedAt(source.getModifiedAt());
    }
}