package pl.databucket.mapper;

import org.modelmapper.PropertyMap;
import pl.databucket.dto.AccessTreeBucketDto;
import pl.databucket.entity.Bucket;

public class AccessTreeBucketPropertyMap extends PropertyMap<Bucket, AccessTreeBucketDto> {

    @Override
    protected void configure() {
        map().setId(source.getId());
        map().setName(source.getName());
        map().setIconName(source.getIconName());
        map().setClassId(source.getDataClass().getId());
    }
}