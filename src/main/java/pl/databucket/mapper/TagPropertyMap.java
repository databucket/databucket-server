package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.TagDto;
import pl.databucket.entity.Tag;

public class TagPropertyMap extends PropertyMap<Tag, TagDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setBuckets(source.getListOfBuckets());
        map().setDataClasses(source.getListOfDataClasses());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedDate(source.getCreatedDate());
        map().setLastModifiedBy(source.getLastModifiedBy());
        map().setLastModifiedDate(source.getLastModifiedDate());
    }
}