package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.UserDtoResponse;
import pl.databucket.server.entity.User;

public class UserPropertyMap extends PropertyMap<User, UserDtoResponse> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setEnabled(source.getEnabled());
        map().setUsername(source.getUsername());
        map().setTeamsIds(source.getTeamsIds());
        map().setExpirationDate(source.getExpirationDate());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedAt(source.getCreatedAt());
        map().setModifiedBy(source.getModifiedBy());
        map().setModifiedAt(source.getModifiedAt());
    }
}