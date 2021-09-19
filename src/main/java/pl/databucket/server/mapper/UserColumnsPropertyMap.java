package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.UserColumnsDto;
import pl.databucket.server.entity.DataColumns;

public class UserColumnsPropertyMap extends PropertyMap<DataColumns, UserColumnsDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setConfiguration(source.getConfiguration());
    }
}