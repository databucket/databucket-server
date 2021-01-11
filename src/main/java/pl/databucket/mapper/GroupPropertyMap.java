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
        map().setBucketsIds(source.getListOfBuckets());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedDate(source.getCreatedDate());
        map().setLastModifiedBy(source.getLastModifiedBy());
        map().setLastModifiedDate(source.getLastModifiedDate());
    }
}