package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.UserFilterDto;
import pl.databucket.entity.DataFilter;

public class UserFilterPropertyMap extends PropertyMap<DataFilter, UserFilterDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setConfiguration(source.getConfiguration());
    }
}