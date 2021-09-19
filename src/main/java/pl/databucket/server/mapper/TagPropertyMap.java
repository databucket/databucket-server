package pl.databucket.server.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.server.dto.TagDto;
import pl.databucket.server.entity.Tag;

public class TagPropertyMap extends PropertyMap<Tag, TagDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setDescription(source.getDescription());
        map().setBucketsIds(source.getBucketsIds());
        map().setClassesIds(source.getDataClassesIds());

        map().setCreatedBy(source.getCreatedBy());
        map().setCreatedAt(source.getCreatedAt());
        map().setModifiedBy(source.getModifiedBy());
        map().setModifiedAt(source.getModifiedAt());
    }
}