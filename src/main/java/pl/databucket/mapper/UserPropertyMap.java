package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.UserDtoResponse;
import pl.databucket.entity.User;

public class UserPropertyMap extends PropertyMap<User, UserDtoResponse> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setChangePassword(source.isChangePassword());
        map().setEnabled(source.getEnabled());
        map().setRoles(source.getRolesIds());
    }
}