package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.AccessTreeGroupDto;
import pl.databucket.server.entity.Group;

public class AccessTreeGroupPropertyMap extends PropertyMap<Group, AccessTreeGroupDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setShortName(source.getShortName());
        map().setBucketsIds(source.getBucketsIds());
    }
}