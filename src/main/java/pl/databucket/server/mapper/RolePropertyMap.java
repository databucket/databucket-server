package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.RoleDto;
import pl.databucket.server.entity.Role;

public class RolePropertyMap extends PropertyMap<Role, RoleDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
    }
}