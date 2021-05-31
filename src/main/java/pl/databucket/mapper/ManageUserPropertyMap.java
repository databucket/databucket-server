package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.ManageUserDtoResponse;
import pl.databucket.entity.User;

public class ManageUserPropertyMap extends PropertyMap<User, ManageUserDtoResponse> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setUsername(source.getUsername());
        map().setEnabled(source.getEnabled());
        map().setExpirationDate(source.getExpirationDate());
        map().setRolesIds(source.getRolesIds());
        map().setProjectsIds(source.getProjectsIds());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedAt(source.getCreatedAt());
        map().setModifiedBy(source.getModifiedBy());
        map().setModifiedAt(source.getModifiedAt());
    }
}