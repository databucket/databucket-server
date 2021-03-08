package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.UserDtoResponse;
import pl.databucket.entity.User;

public class UserPropertyMap extends PropertyMap<User, UserDtoResponse> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setEnabled(source.getEnabled());
        map().setUsername(source.getUsername());
        map().setTeamsIds(source.getTeamsIds());
        map().setExpirationDate(source.getExpirationDate());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedDate(source.getCreatedDate());
        map().setLastModifiedBy(source.getLastModifiedBy());
        map().setLastModifiedDate(source.getLastModifiedDate());
    }
}