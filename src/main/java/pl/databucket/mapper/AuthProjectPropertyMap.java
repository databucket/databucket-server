package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.AuthProjectDTO;
import pl.databucket.entity.Project;

public class AuthProjectPropertyMap extends PropertyMap<Project, AuthProjectDTO> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setEnabled(source.getEnabled());
        map().setExpired(source.isExpired());
    }
}