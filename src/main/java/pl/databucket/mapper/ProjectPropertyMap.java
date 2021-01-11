package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.ProjectDto;
import pl.databucket.entity.Project;

public class ProjectPropertyMap extends PropertyMap<Project, ProjectDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedDate(source.getCreatedDate());
        map().setLastModifiedBy(source.getLastModifiedBy());
        map().setLastModifiedDate(source.getLastModifiedDate());
    }
}