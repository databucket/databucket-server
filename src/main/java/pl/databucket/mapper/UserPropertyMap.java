package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.UserDtoResponse;
import pl.databucket.entity.User;

public class UserPropertyMap extends PropertyMap<User, UserDtoResponse> {

    @Override
    protected void configure() {
        map().setName(source.getName());
        map().setEnabled(source.getEnabled());
        map().setRolesIds(source.getRolesIds());
        map().setProjectsIds(source.getProjectsIds());
        map().setGroupsIds(source.getGroupsIds());
        map().setBucketsIds(source.getBucketsIds());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedDate(source.getCreatedDate());
        map().setLastModifiedBy(source.getLastModifiedBy());
        map().setLastModifiedDate(source.getLastModifiedDate());
    }
}