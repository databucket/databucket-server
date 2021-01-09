package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.AuthProjectDto;
import pl.databucket.entity.Project;

public class AuthProjectPropertyMap extends PropertyMap<Project, AuthProjectDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
    }
}