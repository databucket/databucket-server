package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.RoleDto;
import pl.databucket.entity.Role;

public class RolePropertyMap extends PropertyMap<Role, RoleDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
    }
}