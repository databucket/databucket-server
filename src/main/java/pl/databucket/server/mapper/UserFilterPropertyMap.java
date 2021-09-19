package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.UserFilterDto;
import pl.databucket.server.entity.DataFilter;

public class UserFilterPropertyMap extends PropertyMap<DataFilter, UserFilterDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setConfiguration(source.getConfiguration());
    }
}