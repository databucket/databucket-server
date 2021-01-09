package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.GroupDto;
import pl.databucket.entity.Group;

public class GroupPropertyMap extends PropertyMap<Group, GroupDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setBuckets(source.getListOfBuckets());
    }
}