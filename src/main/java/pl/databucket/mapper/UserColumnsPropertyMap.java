package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.UserColumnsDto;
import pl.databucket.entity.DataColumns;

public class UserColumnsPropertyMap extends PropertyMap<DataColumns, UserColumnsDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setConfiguration(source.getConfiguration());
    }
}